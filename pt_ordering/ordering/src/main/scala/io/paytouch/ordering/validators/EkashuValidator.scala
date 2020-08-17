package io.paytouch.ordering.validators

import cats.data.Validated.{ Invalid, Valid }
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.model.{ CartRecord, EkashuConfig }
import io.paytouch.ordering.ekashu.EkashuEncodings
import io.paytouch.ordering.entities.ekashu.SuccessPayload
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.errors.{
  InvalidPaymentProcessorHashCodeResult,
  InvalidPaymentProcessorReference,
  MissingPaymentProcessorConfig,
  PaymentProcessorTotalMismatch,
}
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

import scala.concurrent.{ ExecutionContext, Future }

class EkashuValidator(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends EkashuEncodings
       with LazyLogging {

  type Record = CartValidator#Record

  private val cartValidator = new CartValidator(ptCoreClient)
  private val merchantDao = daos.merchantDao

  def validateSuccessPayload(payload: SuccessPayload): Future[ValidatedData[Record]] =
    payload.ekashuReference match {
      case Some(reference) =>
        cartValidator.exists(reference).flatMap {
          case Valid(cart) => validatePayload(cart, payload)
          case Invalid(_) =>
            Future.successful(
              ValidatedData.failure(InvalidPaymentProcessorReference(PaymentProcessor.Ekashu, Some(reference))),
            )
        }
      case _ =>
        Future.successful(ValidatedData.failure(InvalidPaymentProcessorReference(PaymentProcessor.Ekashu, None)))
    }

  private def validatePayload(cart: CartRecord, payload: SuccessPayload): Future[ValidatedData[Record]] =
    merchantDao.findPaymentProcessorConfig(cart.merchantId).map {
      case Some(config: EkashuConfig) =>
        implicit val c = config
        val validCartTotal = validateCartTotal(cart, payload)
        val validHashCodeResult = validateHashCodeResult(cart, payload)
        ValidatedData.combine(validCartTotal, validHashCodeResult) { case _ => cart }
      case _ => ValidatedData.failure(MissingPaymentProcessorConfig(PaymentProcessor.Ekashu, cart.merchantId))
    }

  private def validateCartTotal(cart: CartRecord, payload: SuccessPayload): ValidatedData[Record] =
    if (cart.totalAmount == payload.ekashuAmount) ValidatedData.success(cart)
    else
      ValidatedData.failure(
        PaymentProcessorTotalMismatch(PaymentProcessor.Ekashu, payload.ekashuAmount, cart.id, cart.totalAmount),
      )

  private def validateHashCodeResult(
      cart: CartRecord,
      payload: SuccessPayload,
    )(implicit
      ekashuConfig: EkashuConfig,
    ): ValidatedData[Record] = {
    val expected = calculateEkashuHashSuccessResult(payload.ekashuTransactionID)
    val actual = payload.ekashuHashCodeResult
    if (actual.contains(expected)) ValidatedData.success(cart)
    else {
      val msg =
        s"""[Ekashu Code Result $actual] Mismatch in ekashu_hash_code_result.
           |Actual: $actual, Expected: $expected""".stripMargin
      logger.error(msg)
      ValidatedData.failure(InvalidPaymentProcessorHashCodeResult(PaymentProcessor.Ekashu, actual))
    }
  }

}
