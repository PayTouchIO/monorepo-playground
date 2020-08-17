package io.paytouch.ordering.services

import java.util.{ Currency, UUID }

import akka.http.scaladsl.model.Uri
import cats.data.OptionT
import cats.data.Validated.{ Invalid, Valid }
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering._
import io.paytouch.ordering.clients.paytouch.core.entities.Location
import io.paytouch.ordering.clients.worldpay.{ WorldpayCheckoutUri, WorldpayClient }
import io.paytouch.ordering.clients.worldpay.entities._
import io.paytouch.ordering.conversions.WorldpayConversions
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.model.{
  CartRecord,
  PaymentIntentRecord,
  PaymentProcessorCallbackUpdate,
  WorldpayConfig,
  WorldpayPaymentRecord,
  WorldpayPaymentType,
  WorldpayPaymentUpdate,
}
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.worldpay._
import io.paytouch.ordering.entities.enums.{
  CartStatus,
  PaymentIntentStatus,
  PaymentProcessor,
  PaymentProcessorCallbackStatus,
}
import io.paytouch.ordering.errors._
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.utils.ResultType
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedOptData
import io.paytouch.ordering.utils.validation.ValidatedOptData.ValidatedOptData
import io.paytouch.ordering.withTag

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }
import cats.data.Validated.{ Invalid, Valid }

class WorldpayService(
    val authenticationService: AuthenticationService,
    val cartService: CartService,
    val cartSyncService: CartSyncService,
    paymentIntentService: => PaymentIntentService,
    val client: WorldpayClient,
    val locationService: LocationService,
    val checkoutUri: Uri withTag WorldpayCheckoutUri,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends WorldpayConversions
       with LazyLogging {

  type Context = StoreContext
  type Record = WorldpayPaymentRecord
  type Params = Map[String, String]

  private val dao = daos.worldpayPaymentDao
  private val merchantDao = daos.merchantDao
  private val callbackDao = daos.paymentProcessorCallbackDao
  private val cartDao = daos.cartDao
  private val paymentIntentDao = daos.paymentIntentDao

  val CompleteStatus = "Complete"
  val CancelledStatus = "Cancelled"

  def setupTransaction(
      cartRecord: CartRecord,
      successReturnUrl: String,
      failureReturnUrl: String,
      worldpayConfig: WorldpayConfig,
    )(implicit
      context: Context,
    ): Future[ValidatedData[PaymentProcessorData]] = {
    val validated = for {
      validCurrency <- validateCurrency()
      validLocation <- validateLocation()
    } yield ValidatedData.combine(validCurrency, validLocation) { (_, maybeLocation) =>
      (worldpayConfig, cartRecord, maybeLocation)
    }

    validated.flatMap {
      case Valid((config, cart, maybeLocation)) =>
        val orderId = cart.orderId.get
        val total = MonetaryAmount(cart.totalAmount, cart.currency)
        val storeName = maybeLocation.map(_.name)
        client.transactionSetup(config, orderId, total, storeName).flatMap {
          case (clientResponse: TransactionSetupResponse) =>
            val model = cartToUpsertion(cart, successReturnUrl, failureReturnUrl, clientResponse)
            dao.upsert(model).map {
              case (_, record) =>
                ValidatedData.success(recordToPaymentProcessorData(record))
            }

          case (response: WorldpayResponse) =>
            Future.successful {
              ValidatedData.failure(WorldpayClientError(response))
            }
        }

      case i @ Invalid(_) => Future.successful(i)
    }
  }

  def setupTransaction(
      paymentIntent: PaymentIntentRecord,
      worldpayConfig: WorldpayConfig,
    )(implicit
      context: RapidoOrderContext,
    ): Future[ValidatedData[PaymentProcessorData]] =
    validateCurrency().flatMap {
      case Valid(currency) =>
        val orderId = paymentIntent.orderId
        val total = MonetaryAmount(paymentIntent.totalAmount, context.currency)
        val storeName = context.order.location.map(_.name)
        client.transactionSetup(worldpayConfig, paymentIntent.orderId, total, storeName).flatMap {
          case (clientResponse: TransactionSetupResponse) =>
            val model =
              paymentIntentToUpsertion(paymentIntent, clientResponse)
            dao.upsert(model).map {
              case (_, record) =>
                ValidatedData.success(recordToPaymentProcessorData(record))
            }

          case (response: WorldpayResponse) =>
            Future.successful {
              ValidatedData.failure(WorldpayClientError(response))
            }
        }

      case i @ Invalid(_) => Future.successful(i)
    }

  def receive(
      transactionSetupId: String,
      status: WorldpayPaymentStatus,
      params: Params,
    ): Future[ValidatedData[Uri]] =
    dao.findByTransactionSetupId(transactionSetupId).flatMap {
      case Some(payment) =>
        payment.status match {
          case WorldpayPaymentStatus.Submitted =>
            payment.objectType match {
              case WorldpayPaymentType.Cart =>
                receiveCart(payment, status, params)

              case WorldpayPaymentType.PaymentIntent =>
                receivePaymentIntent(payment, status, params)
            }

          // If the payment is already marked as completed or cancelled,
          // redirect the user and skip everything else
          case WorldpayPaymentStatus.Complete =>
            Future.successful(ValidatedData.success(payment.successReturnUrl))
          case WorldpayPaymentStatus.Cancelled =>
            Future.successful(ValidatedData.success(payment.failureReturnUrl))
        }
      case None =>
        Future.successful(ValidatedData.failure(WorldpayPaymentNotFound(transactionSetupId)))
    }

  //
  // Cart
  //

  private def validateLocation()(implicit context: Context): Future[ValidatedOptData[Location]] =
    locationService.findByStoreContext().map {
      case Some(location) => ValidatedOptData.successOpt(location)
      case _              => ValidatedOptData.empty
    }

  private def receiveCart(
      payment: WorldpayPaymentRecord,
      status: WorldpayPaymentStatus,
      params: Params,
    ): Future[ValidatedData[Uri]] =
    storeContextFromWorldpayPayment(payment).flatMap {
      case Some(ctx) =>
        implicit val context: Context = ctx

        val validated = for {
          validConfig <- validatePaymentProcessorConfig()
          validCart <- validateCart(payment.objectId)
        } yield ValidatedData.combine(validConfig, validCart)((config, cart) => (config, cart))

        validated.flatMap {
          case Valid((config, cart)) =>
            registerCallback(payment.objectId, params, status)
            queryTransaction(payment, config, status) { response =>
              validateApprovedAmount(cart, response) match {
                case Valid(_)       => syncCartAsPaid(payment, cart, response)
                case i @ Invalid(_) => Future.successful(i)
              }
            }
          case i @ Invalid(_) => Future.successful(i)
        }

      case None =>
        Future.successful(
          ValidatedData
            .failure(ImpossibleState(s"Couldn't create store context for worldpay payment = ${payment.id}")),
        )
    }

  private def storeContextFromWorldpayPayment(payment: WorldpayPaymentRecord): Future[Option[StoreContext]] =
    authenticationService.getStoreContextFromCartId(payment.objectId)

  private def validateCart(cartId: UUID)(implicit context: Context): Future[ValidatedData[CartRecord]] =
    cartDao.findById(cartId).map {
      case Some(cart: CartRecord) =>
        cart.status match {
          case CartStatus.Paid =>
            logger.error(
              s"Worldpay Receive callback for a cart that is already marked as paid cartId = ${cart.id}. Continuing as if cart is new...",
            )
            ValidatedData.success(cart)

          case _ =>
            ValidatedData.success(cart)
        }

      case _ =>
        ValidatedData.failure(ImpossibleState(s"Cart $cartId does not exist"))
    }

  private def validateApprovedAmount(cart: CartRecord, response: TransactionQueryResponse): ValidatedData[BigDecimal] =
    if (cart.totalAmount == response.approvedAmount)
      ValidatedData.success(response.approvedAmount)
    else
      ValidatedData.failure(
        PaymentProcessorTotalMismatch(PaymentProcessor.Worldpay, response.approvedAmount, cart.id, cart.totalAmount),
      )

  private def syncCartAsPaid(
      payment: WorldpayPaymentRecord,
      cart: CartRecord,
      response: TransactionQueryResponse,
    )(implicit
      context: Context,
    ): Future[ValidatedData[Uri]] =
    for {
      _ <- updatePaymentStatus(payment, WorldpayPaymentStatus.Complete)
      _ <- handleResponse(cart, response)
    } yield ValidatedData.success(payment.successReturnUrl)

  private def handleResponse(
      cart: CartRecord,
      response: TransactionQueryResponse,
    )(implicit
      context: Context,
    ): Future[UpsertionResult[CartRecord]] =
    if (ServiceConfigurations.featureConfig.useStorePaymentTransaction.value)
      cartSyncService.storePaymentTransaction(
        cart,
        cart.orderId.get,
        paymentTransactionUpsertion(response, cart.tipAmount),
      )
    else
      cartSyncService.sync(
        cart,
        addPaymentTransactionToOrderUpsertion(response),
      )

  //
  // Payment Intent
  //

  def receivePaymentIntent(
      payment: WorldpayPaymentRecord,
      status: WorldpayPaymentStatus,
      params: Params,
    ): Future[ValidatedData[Uri]] =
    rapidoContextFromWorldpayPayment(payment).flatMap {
      case Some((ctx, paymentIntent)) =>
        implicit val context: RapidoOrderContext = ctx

        val validated = for {
          validConfig <- validatePaymentProcessorConfig()
          validPaymentIntent <- validatePaymentIntent(paymentIntent)
        } yield ValidatedData.combine(validConfig, validPaymentIntent)((config, paymentIntent) =>
          (config, paymentIntent),
        )

        validated.flatMap {
          case Valid((config, paymentIntent)) =>
            registerCallback(payment.objectId, params, status)
            queryTransaction(payment, config, status) { response =>
              validateApprovedAmount(paymentIntent, response) match {
                case Valid(_)       => syncPaymentIntentAsPaid(payment, paymentIntent, response)
                case i @ Invalid(_) => Future.successful(i)
              }
            }
          case i @ Invalid(_) => Future.successful(i)
        }

      case None =>
        Future.successful(
          ValidatedData
            .failure(ImpossibleState(s"Couldn't create rapido context for worldpay payment = ${payment.id}")),
        )
    }

  private def rapidoContextFromWorldpayPayment(
      payment: WorldpayPaymentRecord,
    ): Future[Option[(RapidoOrderContext, PaymentIntentRecord)]] =
    paymentIntentDao
      .findById(payment.objectId)
      .flatMap {
        case Some(paymentIntent) =>
          authenticationService
            .getRapidoOrderContext(paymentIntent.merchantId, paymentIntent.orderId)
            .map(_.map(_ -> paymentIntent))

        case _ =>
          Future.successful(None)
      }

  private def validatePaymentIntent(paymentIntent: PaymentIntentRecord): Future[ValidatedData[PaymentIntentRecord]] =
    Future.successful {
      paymentIntent.status match {
        case PaymentIntentStatus.Paid =>
          logger.error(
            s"Worldpay Receive callback for a payment intent that is already marked as paid paymentIntentId = ${paymentIntent.id}. Continuing as if payment intent is new...",
          )
          ValidatedData.success(paymentIntent)

        case _ =>
          ValidatedData.success(paymentIntent)
      }
    }

  private def validateApprovedAmount(
      paymentIntent: PaymentIntentRecord,
      response: TransactionQueryResponse,
    ): ValidatedData[BigDecimal] =
    if (paymentIntent.totalAmount == response.approvedAmount)
      ValidatedData.success(response.approvedAmount)
    else
      ValidatedData.failure(
        PaymentProcessorTotalMismatch(
          PaymentProcessor.Worldpay,
          response.approvedAmount,
          paymentIntent.id,
          paymentIntent.totalAmount,
        ),
      )

  private def syncPaymentIntentAsPaid(
      payment: WorldpayPaymentRecord,
      paymentIntent: PaymentIntentRecord,
      response: TransactionQueryResponse,
    )(implicit
      context: RapidoOrderContext,
    ): Future[ValidatedData[Uri]] = {
    val tipAmount = 0 // Tips aren't currently supported on Rapido
    for {
      _ <- updatePaymentStatus(payment, WorldpayPaymentStatus.Complete)
      _ <- paymentIntentService.markAsPaid(paymentIntent, paymentTransactionUpsertion(response, tipAmount))
    } yield ValidatedData.success(payment.successReturnUrl)
  }

  //
  // Generic
  //

  private def queryTransaction(
      payment: WorldpayPaymentRecord,
      config: WorldpayConfig,
      status: WorldpayPaymentStatus,
    )(
      f: TransactionQueryResponse => Future[ValidatedData[Uri]],
    )(implicit
      context: AppContext,
    ): Future[ValidatedData[Uri]] =
    status match {
      case WorldpayPaymentStatus.Complete =>
        client
          .transactionQuery(config, payment.transactionSetupId)
          .flatMap {
            case response: TransactionQueryResponse =>
              f(response)

            case response: WorldpayResponse =>
              Future.successful(ValidatedData.failure(WorldpayClientError(response)))
          }

      case _ =>
        updatePaymentStatus(payment, WorldpayPaymentStatus.Cancelled)
          .as(ValidatedData.success(payment.failureReturnUrl))
    }

  private def validatePaymentProcessorConfig()(implicit context: AppContext): Future[ValidatedData[WorldpayConfig]] =
    merchantDao.findPaymentProcessorConfig(context.merchantId).map {
      case Some(config: WorldpayConfig) =>
        ValidatedData.success(config)

      case _ =>
        ValidatedData.failure(MissingPaymentProcessorConfig(PaymentProcessor.Worldpay, context.merchantId))
    }

  private def validateCurrency()(implicit context: AppContext): Future[ValidatedData[Currency]] =
    Future {
      if (context.currency == Currency.getInstance("USD"))
        ValidatedData.success(context.currency)
      else
        ValidatedData.failure(
          UnsupportedPaymentProcessorCurrency(PaymentProcessor.Worldpay, context.currency, context.merchantId),
        )
    }

  private def registerCallback(
      objectId: UUID,
      fields: Params,
      status: WorldpayPaymentStatus,
    ): Unit = {
    val update: PaymentProcessorCallbackUpdate = PaymentProcessorCallbackUpdate(
      id = None,
      paymentProcessor = Some(PaymentProcessor.Worldpay),
      status = Some(status.genericStatus),
      reference = Some(objectId.toString),
      payload = Some(JsonSupport.fromEntityToJValue(fields)),
    )

    callbackDao.upsert(update) onComplete {
      case Success(_) =>
        logger.info(s"[$objectId] saved worldpay callback with status $status")

      case Failure(ex) =>
        logger.error(s"Couldn't save worldpay callback {status -> $status; fields -> $fields}", ex)
    }
  }

  private def updatePaymentStatus(payment: WorldpayPaymentRecord, status: WorldpayPaymentStatus) = {
    val update = WorldpayPaymentUpdate(
      id = payment.id,
      status = Some(status),
    )

    dao.upsert(update)
  }
}
