package io.paytouch.ordering.services

import cats.data.Validated.{ Invalid, Valid }
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.conversions.JetdirectConversions
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.model.{ CartRecord, EkashuConfig, JetdirectConfig, PaymentProcessorCallbackUpdate }
import io.paytouch.ordering.entities.{ AppContext, PaymentProcessorData }
import io.paytouch.ordering.entities.enums.{ PaymentProcessor, PaymentProcessorCallbackStatus }
import io.paytouch.ordering.entities.jetdirect.CallbackPayload
import io.paytouch.ordering.jetdirect.JetdirectParser
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.validators.JetdirectValidator

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class JetdirectService(
    val ptCoreClient: PtCoreClient,
    val cartSyncService: CartSyncService,
    val merchantService: MerchantService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends JetdirectConversions
       with JetdirectParser
       with LazyLogging {

  private val validator = new JetdirectValidator(ptCoreClient)
  private val storeDao = daos.storeDao
  private val paymentProcessorCallbackDao = daos.paymentProcessorCallbackDao

  def processCallback(
      fields: Map[String, String],
      status: PaymentProcessorCallbackStatus,
    ): Future[ValidatedData[Unit]] = {
    registerCallback(fields, status)
    parseJetdirectEntity(fields) match {
      case Valid(p) if p.status == PaymentProcessorCallbackStatus.Success => successCallback(p)
      case Valid(_)                                                       => Future.successful(ValidatedData.success(()))
      case i @ Invalid(_)                                                 => Future.successful(i)
    }
  }

  private def successCallback(payload: CallbackPayload): Future[ValidatedData[Unit]] =
    validator.validateSuccessPayload(payload).flatMapValid { cart =>
      storeDao.findStoreContextByCartId(cart.id).flatMap {
        case Some(context) =>
          cartSyncService.sync(cart, addPaymentTransactionToOrderUpsertion(payload))(context).map(_ => ())
        case None => Future.unit
      }
    }

  private def registerCallback(fields: Map[String, String], status: PaymentProcessorCallbackStatus): Unit = {
    val update: PaymentProcessorCallbackUpdate = PaymentProcessorCallbackUpdate(
      id = None,
      paymentProcessor = Some(PaymentProcessor.Jetdirect),
      status = Some(status),
      reference = fields.get("order_number"),
      payload = Some(JsonSupport.fromEntityToJValue(fields)),
    )
    paymentProcessorCallbackDao.upsert(update) onComplete {
      case Success(_) =>
        val cartId = update.reference.toOption.getOrElse("Unknown")
        logger.info(s"[Cart $cartId] saved jetDirect callback with status $status")
      case Failure(ex) =>
        logger.error(s"Couldn't save jetDirect callback {status -> $status; fields -> $fields}", ex)
    }
  }

  def computeJetdirectHashCodeByCart(cartRecord: CartRecord, jetDirectConfig: JetdirectConfig): PaymentProcessorData =
    fromCartRecordToPaymentProcessorData(cartRecord)(jetDirectConfig)
}
