package io.paytouch.ordering.services

import cats.data.Validated.{ Invalid, Valid }
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.conversions.EkashuConversions
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.model.{ CartRecord, EkashuConfig, PaymentProcessorCallbackUpdate }
import io.paytouch.ordering.ekashu.EkashuParser
import io.paytouch.ordering.entities.ekashu.SuccessPayload
import io.paytouch.ordering.entities.enums.{ PaymentProcessor, PaymentProcessorCallbackStatus }
import io.paytouch.ordering.entities.{ AppContext, PaymentProcessorData }
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.validators.EkashuValidator

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class EkashuService(
    val ptCoreClient: PtCoreClient,
    val cartSyncService: CartSyncService,
    val merchantService: MerchantService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends EkashuConversions
       with EkashuParser
       with LazyLogging {

  private val validator = new EkashuValidator(ptCoreClient)
  private val storeDao = daos.storeDao
  private val ekashuCallbackDao = daos.paymentProcessorCallbackDao

  def processCallback(
      fields: Map[String, String],
      status: PaymentProcessorCallbackStatus,
    ): Future[ValidatedData[Unit]] = {
    registerCallback(fields, status)
    parseEkashuEntity(fields) match {
      case Valid(p)       => successCallback(p)
      case i @ Invalid(_) => Future.successful(i)
    }
  }

  private def successCallback(payload: SuccessPayload): Future[ValidatedData[Unit]] =
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
      paymentProcessor = Some(PaymentProcessor.Ekashu),
      status = Some(status),
      reference = fields.get("ekashu_reference"),
      payload = Some(JsonSupport.fromEntityToJValue(fields)),
    )
    ekashuCallbackDao.upsert(update) onComplete {
      case Success(_) =>
        val cartId = update.reference.toOption.getOrElse("Unknown")
        logger.info(s"[Cart $cartId] saved ekashu callback with status $status")
      case Failure(ex) =>
        logger.error(s"Couldn't save ekashu callback {status -> $status; fields -> $fields}", ex)
    }
  }

  def computeEkashuHashCodeByCart(cartRecord: CartRecord, ekashuConfig: EkashuConfig): PaymentProcessorData =
    fromCartRecordToPaymentProcessorData(cartRecord)(ekashuConfig)
}
