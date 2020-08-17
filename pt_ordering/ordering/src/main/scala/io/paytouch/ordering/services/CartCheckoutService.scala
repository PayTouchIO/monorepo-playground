package io.paytouch.ordering.services

import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.ordering._
import io.paytouch.ordering.clients._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums._
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.data.model._
import io.paytouch.ordering.entities.enums.PaymentMethodType
import io.paytouch.ordering.entities.GiftCardPassApplied
import io.paytouch.ordering.errors._
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.UpsertionResult
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

class CartCheckoutService(
    val cartService: CartService,
    val cartSyncService: CartSyncService,
    val merchantService: MerchantService,
    val worldpayService: WorldpayService,
    val stripeService: StripeService,
    val ekashuService: EkashuService,
    val jetdirectService: JetdirectService,
    val coreClient: PtCoreClient,
  )(implicit
    val ec: ExecutionContext,
  ) extends LazyLogging {
  type ReturnUrls = (String, String)

  def checkout(
      id: UUID,
      update: entities.CartUpdate,
    )(implicit
      context: entities.StoreContext,
    ): Future[UpsertionResult[entities.Cart]] =
    updateAndValidate(id, update).onValid {
      case (cartRecord, returnUrls) =>
        cartSyncService.validatedSync(cartRecord, (_, upsertion) => paymentTypeConversion(upsertion)).onValid {
          case (result, cartRecord) =>
            bulkChargeGiftCardPasses(cartRecord).onValid { _ =>
              getPaymentProcessorData(cartRecord, returnUrls).onValid { paymentProcessorData =>
                cartService.enrich(cartRecord).map { cart =>
                  Validated.Valid(
                    result -> cart.copy(paymentProcessorData = paymentProcessorData.some),
                  )
                }
              }
            }
        }
    }

  def bulkChargeGiftCardPasses(
      cartRecord: CartRecord,
    )(implicit
      context: entities.StoreContext,
    ): Future[ValidatedData[Unit]] =
    if (cartRecord.appliedGiftCardPasses.isEmpty)
      ValidatedData.success(()).pure[Future]
    else {
      val (alreadyCharged, notChargedYet) =
        cartRecord
          .appliedGiftCardPasses
          .partition(_.paymentTransactionId.isDefined)

      def message(passes: Seq[GiftCardPassApplied]) =
        passes
          .map(_.onlineCode.value)
          .mkString(", ")

      if (alreadyCharged.nonEmpty)
        logger.warn(
          s"""|These gift cards were already charged: "${message(alreadyCharged)}" for cart: "${cartRecord.id}".
              |But there are still more to charge: "${message(notChargedYet)}". Charging now...""".stripMargin,
        )

      doBulkChargeGiftCardPasses(cartRecord, alreadyCharged, notChargedYet)
    }

  private def doBulkChargeGiftCardPasses(
      cartRecord: CartRecord,
      alreadyCharged: Seq[GiftCardPassApplied],
      notChargedYet: Seq[GiftCardPassApplied],
    )(implicit
      context: entities.StoreContext,
    ): Future[ValidatedData[Unit]] =
    coreClient
      .giftCardPassesBulkCharge(
        orderId = OrderIdPostgres(cartRecord.orderId.get).cast,
        bulkCharge = for {
          pass <- notChargedYet
          amount <- pass.amountToCharge
        } yield GiftCardPassCharge(pass.id, amount.amount),
      )(coreClient.generateAuthHeaderForCore)
      .flatMap {
        case Right(response) =>
          val order =
            response.data

          val paymentDetails =
            order
              .paymentTransactions
              .map(_.paymentDetails)

          val appliedGiftCardPassesWithTransactionIds =
            notChargedYet
              .map { pass =>
                pass.copy(
                  paymentTransactionId = paymentDetails
                    .find(_.giftCardPassId.contains(pass.id.cast.get.value))
                    .flatMap(_.giftCardPassTransactionId),
                )
              }

          cartSyncService
            .upsertCart(
              cartId = cartRecord.id,
              appliedGiftCardPasses = (alreadyCharged ++ appliedGiftCardPassesWithTransactionIds)
                .sortBy(_.addedAt)
                .some,
            )(order)
            .as(ValidatedData.success(()))

        case Left(clientError) =>
          val allErrors =
            clientError.errors.flatMap(_.convert)

          val bulkFailure =
            allErrors
              .collect { case e: InsufficientFunds => e }
              .flatMap(_.bulkFailure)

          cartService
            .applyGiftCardFailures(cartRecord, bulkFailure)
            .as {
              NonEmptyList.fromList(allErrors.toList) match {
                case Some(l) => Validated.Invalid(l)
                case _       => Validated.Invalid(NonEmptyList.of(clientError))
              }
            }
      }

  private def updateAndValidate(
      id: UUID,
      update: entities.CartUpdate,
    )(implicit
      context: entities.StoreContext,
    ): Future[ValidatedData[(CartRecord, ReturnUrls)]] =
    for {
      validCart <- cartService.updateAndValidate(id, update)
      validReturnUrls <- validateReturnUrls(update).pure[Future]
    } yield (validCart, validReturnUrls).tupled

  private def validateReturnUrls(update: entities.CartUpdate): ValidatedData[ReturnUrls] =
    (update.checkoutSuccessReturnUrl, update.checkoutFailureReturnUrl)
      .tupled
      .map(ValidatedData.success)
      .getOrElse(ValidatedData.failure(MissingCheckoutReturnUrl()))

  private def getPaymentProcessorData(
      cart: CartRecord,
      returnUrls: ReturnUrls,
    )(implicit
      context: entities.StoreContext,
    ): Future[ValidatedData[entities.PaymentProcessorData]] =
    if (cashOrEntireTotalCoveredByAppliedGiftCards(cart))
      ValidatedData.success(entities.PaymentProcessorData.empty).pure[Future]
    else
      // The cart validations check the payment type is supported, so we don't
      // need to return an error if the merchant payment processor config and
      // cart payment method don't match
      merchantService.getPaymentProcessorConfig(context).flatMap {
        case Some(ekashuConfig: EkashuConfig) =>
          ValidatedData.success(ekashuService.computeEkashuHashCodeByCart(cart, ekashuConfig)).pure[Future]

        case Some(jetdirectConfig: JetdirectConfig) =>
          ValidatedData.success(jetdirectService.computeJetdirectHashCodeByCart(cart, jetdirectConfig)).pure[Future]

        case Some(worldpayConfig: WorldpayConfig) =>
          val (successReturnUrl, failureReturnUrl) = returnUrls

          worldpayService.setupTransaction(cart, successReturnUrl, failureReturnUrl, worldpayConfig)

        case Some(stripeConfig: StripeConfig) =>
          stripeService.createPaymentIntent(cart, stripeConfig)

        case _ =>
          ValidatedData.success(entities.PaymentProcessorData.empty).pure[Future]
      }

  private def cashOrEntireTotalCoveredByAppliedGiftCards(cart: CartRecord): Boolean =
    Boolean.or(
      cart.paymentMethodType.contains(PaymentMethodType.Cash),
      cart.totalAmount == 0 && cart.appliedGiftCardPasses.nonEmpty,
    )

  private def paymentTypeConversion(orderUpsertion: OrderUpsertion): OrderUpsertion =
    orderUpsertion.paymentType match {
      case Some(OrderPaymentType.Cash) =>
        // cartSyncService marks the cart as paid when acceptanceStatus == pending
        orderUpsertion.copy(
          onlineOrderAttribute = orderUpsertion
            .onlineOrderAttribute
            .copy(
              acceptanceStatus = AcceptanceStatus.Pending,
            ),
        )

      case _ =>
        orderUpsertion
    }
}
