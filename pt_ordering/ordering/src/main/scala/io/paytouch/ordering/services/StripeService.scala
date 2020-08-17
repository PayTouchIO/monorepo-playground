package io.paytouch.ordering.services

import java.util.UUID

import scala.concurrent._
import scala.util._

import cats.data._
import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.implicits._

import io.paytouch.ordering._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.PaymentStatus
import io.paytouch.ordering.clients.stripe.entities._
import io.paytouch.ordering.clients.stripe.{ StripeClient, StripeClientConfig }
import io.paytouch.ordering.conversions.StripeConversions
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.model.{ CartRecord, PaymentProcessorCallbackUpdate, StripeConfig }
import io.paytouch.ordering.entities.enums.{ PaymentProcessor, PaymentProcessorCallbackStatus }
import io.paytouch.ordering.entities.stripe._
import io.paytouch.ordering.entities.{ MonetaryAmount, PaymentProcessorData, StoreContext }
import io.paytouch.ordering.errors._
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.stripe._
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData._

class StripeService(
    val cartSyncService: CartSyncService,
    val client: StripeClient,
    val config: StripeClientConfig,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends StripeConversions
       with LazyLogging {
  def encodings: StripeEncodings =
    new StripeEncodings {}

  type Context = StoreContext

  private val callbackDao = daos.paymentProcessorCallbackDao
  private val cartDao = daos.cartDao
  private val storeDao = daos.storeDao

  def createPaymentIntent(
      cart: CartRecord,
      merchantConfig: StripeConfig,
    )(implicit
      context: Context,
    ): Future[ValidatedData[PaymentProcessorData]] = {
    val basePointsToDecimalMultiplier = config.applicationFeeBasePoints.value * 0.0001
    val orderId = cart.orderId.get
    val total = MonetaryAmount(cart.totalAmount, cart.currency)
    val applicationFee = total
      .copy(amount = total.amount * basePointsToDecimalMultiplier)

    client
      .createPaymentIntent(merchantConfig, orderId, Some(cart.id), total, applicationFee)
      .map {
        case Left(error) =>
          logger.info(s"While creating payment intent error = $error")
          ValidatedData.failure(StripeClientError(error))

        case Right(paymentIntent) =>
          ValidatedData.success(toPaymentProcessorData(merchantConfig, paymentIntent))
      }
  }

  def processWebhook(
      signature: String,
      payload: String,
      webhook: StripeWebhook,
    ): Future[ValidatedData[Unit]] =
    encodings.validateWebhookSignature(signature, payload, config.webhookSecret).asValidatedFuture { _ =>
      if (config.livemode =!= webhook.livemode) {
        logger.info(s"Expected livemode to be ${config.livemode.value} but it was ${webhook.livemode.value}.")

        ValidatedData.successVoid.pure[Future]
      }
      else
        wasAlreadyHandled(webhook.id).flatMap {
          case true =>
            logger.info(s"Skipping already handled webhook ${webhook.id}.")

            ValidatedData.success(()).pure[Future]

          case false =>
            extractEvent(webhook).asValidatedFuture {
              case event: Event.PaymentIntentSucceededEvent =>
                val reference = event.data.cartId.map(_.toString).getOrElse("")
                registerCallback(reference, signature, webhook, PaymentProcessorCallbackStatus.Success)

                handlePaymentIntentSucceeded(event)

              case event: Event.PaymentIntentPaymentFailedEvent =>
                val reference = event.data.cartId.map(_.toString).getOrElse("")
                registerCallback(reference, signature, webhook, PaymentProcessorCallbackStatus.Declined)

                handlePaymentIntentPaymentFailed(event)
            }
        }
    }

  private def wasAlreadyHandled(webhookId: String): Future[Boolean] =
    callbackDao
      .findByStripeWebhookId(webhookId)
      .map(_.size >= 1)

  protected def extractEvent(webhook: StripeWebhook): Either[Error, Event] =
    Event(webhook)

  def handlePaymentIntentSucceeded(event: Event.PaymentIntentSucceededEvent): Future[ValidatedData[Unit]] = {
    val paymentIntent = event.data

    validateCartId(paymentIntent.cartId).flatMap {
      case Validated.Valid(cart) =>
        storeDao.findStoreContextByCartId(cart.id).flatMap {
          case Some(context) =>
            validateApprovedAmount(cart, paymentIntent) match {
              case Validated.Valid(_) =>
                handlePaymentIntent(cart, paymentIntent)(context).as(ValidatedData.success(()))

              case i @ Validated.Invalid(_) =>
                Future.successful(i)
            }

          case None =>
            // This shouldn't be reached as the cart id is validated before in validateCartId
            Future.successful(
              ValidatedData.failure(PaymentProcessorMissingMandatoryField(PaymentProcessor.Stripe, "cartId")),
            )
        }

      case Validated.Invalid(error) if error.head.code == InvalidPaymentProcessorReference.code =>
        logger.info(s"Received webhook for unknown cartId = ${paymentIntent.cartId}")
        Future.successful(ValidatedData.success((): Unit))

      case i @ Validated.Invalid(_) =>
        Future.successful(i)
    }
  }

  private def handlePaymentIntent(
      cart: CartRecord,
      paymentIntent: PaymentIntent,
    )(implicit
      store: StoreContext,
    ): Future[UpsertionResult[CartRecord]] =
    if (ServiceConfigurations.featureConfig.useStorePaymentTransaction.value)
      cartSyncService
        .storePaymentTransaction(
          cart,
          cart.orderId.get,
          paymentTransactionUpsertion(paymentIntent, cart.tipAmount),
        )
    else
      cartSyncService
        .sync(
          cart,
          addPaymentTransactionToOrderUpsertion(paymentIntent),
        )

  def handlePaymentIntentPaymentFailed(event: Event.PaymentIntentPaymentFailedEvent): Future[ValidatedData[Unit]] = {
    val paymentIntent = event.data

    validateCartId(paymentIntent.cartId).flatMap {
      case Validated.Valid(cart) =>
        storeDao.findStoreContextByCartId(cart.id).flatMap {
          case Some(context) =>
            handlePaymentIntent(cart, paymentIntent)(context).as(ValidatedData.success(()))

          case None =>
            // This shouldn't be reached as the cart id is validated before in validateCartId
            Future.successful(
              ValidatedData.failure(PaymentProcessorMissingMandatoryField(PaymentProcessor.Stripe, "cartId")),
            )
        }

      case Validated.Invalid(error) if error.head.code == InvalidPaymentProcessorReference.code =>
        logger.info(s"Received webhook for unknown cartId = ${paymentIntent.cartId}")
        Future.successful(ValidatedData.success((): Unit))

      case i @ Validated.Invalid(_) =>
        Future.successful(i)
    }
  }

  private def validateCartId(maybeCartId: Option[UUID]): Future[ValidatedData[CartRecord]] =
    maybeCartId match {
      case None =>
        Future.successful(
          ValidatedData.failure(PaymentProcessorMissingMandatoryField(PaymentProcessor.Stripe, "cartId")),
        )

      case Some(cartId) =>
        cartDao.findById(cartId).map {
          case Some(cart) =>
            ValidatedData.success(cart)

          case None =>
            ValidatedData.failure(InvalidPaymentProcessorReference(PaymentProcessor.Stripe, maybeCartId))
        }
    }

  private def validateApprovedAmount(cart: CartRecord, paymentIntent: PaymentIntent): ValidatedData[MonetaryAmount] = {
    val cartTotal = MonetaryAmount(cart.totalAmount, cart.currency)
    val chargeTotal = paymentIntent.charge.get.total

    if (cartTotal == chargeTotal)
      ValidatedData.success(chargeTotal)
    else
      ValidatedData.failure(
        PaymentProcessorTotalMismatch(
          PaymentProcessor.Stripe,
          chargeTotal.amount,
          cart.id,
          cartTotal.amount,
        ),
      )
  }

  private def registerCallback(
      reference: String,
      signature: String,
      webhook: StripeWebhook,
      status: PaymentProcessorCallbackStatus,
    ): Unit = {
    val fields = StripService.CallbackPayload(webhook, signature)

    val update = PaymentProcessorCallbackUpdate(
      id = None,
      paymentProcessor = Some(PaymentProcessor.Stripe),
      status = Some(status),
      reference = Some(reference),
      payload = Some(JsonSupport.fromEntityToJValue(fields)),
    )

    callbackDao.upsert(update) onComplete {
      case Success(_) =>
        logger.info(s"[$reference] saved stripe callback {fields -> $fields}")

      case Failure(ex) =>
        logger.error(s"Couldn't save stripe callback {fields -> $fields, status -> $status}", ex)
    }
  }
}

object StripService {
  final case class CallbackPayload(body: StripeWebhook, signature: String)
}
