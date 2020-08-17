package io.paytouch.ordering.validators

import cats.data.Validated.{ Invalid, Valid }
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.model.{ CartRecord, JetdirectConfig }
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.entities.jetdirect.CallbackPayload
import io.paytouch.ordering.errors._
import io.paytouch.ordering.jetdirect.JetdirectEncodings
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

import scala.concurrent.{ ExecutionContext, Future }

class JetdirectValidator(val ptCoreClient: PtCoreClient)(implicit val ec: ExecutionContext, val daos: Daos)
    extends JetdirectEncodings
       with LazyLogging {

  type Record = CartValidator#Record

  private val cartValidator = new CartValidator(ptCoreClient)
  private val merchantDao = daos.merchantDao

  def validateSuccessPayload(payload: CallbackPayload): Future[ValidatedData[Record]] =
    cartValidator.exists(payload.orderNumber).flatMap {
      case Valid(cart) => validatePayload(cart, payload)
      case Invalid(_) =>
        Future.successful(
          ValidatedData
            .failure(InvalidPaymentProcessorReference(PaymentProcessor.Jetdirect, Some(payload.orderNumber))),
        )
    }

  private def validatePayload(cart: CartRecord, payload: CallbackPayload): Future[ValidatedData[Record]] =
    merchantDao.findPaymentProcessorConfig(cart.merchantId).map {
      case Some(config: JetdirectConfig) =>
        implicit val c = config
        val validCartTotal = validateCartTotal(cart, payload)
        val validCartTip = validateCartTip(cart, payload)
        val validHashCodeResult = validateHashCodeResult(cart, payload)
        ValidatedData.combine(validCartTotal, validCartTip, validHashCodeResult) { case _ => cart }
      case _ => ValidatedData.failure(MissingPaymentProcessorConfig(PaymentProcessor.Jetdirect, cart.merchantId))
    }

  private def validateCartTotal(cart: CartRecord, payload: CallbackPayload): ValidatedData[Record] =
    if (cart.totalAmount - cart.tipAmount == payload.amount) ValidatedData.success(cart)
    else
      ValidatedData.failure(
        PaymentProcessorTotalMismatch(PaymentProcessor.Jetdirect, payload.amount, cart.id, cart.totalAmount),
      )

  private def validateCartTip(cart: CartRecord, payload: CallbackPayload): ValidatedData[Record] =
    payload.tipAmount match {
      case Some(tipAmount) if tipAmount != cart.tipAmount =>
        ValidatedData.failure(PaymentProcessorTipMismatch(PaymentProcessor.Jetdirect, cart, payload.tipAmount))
      case _ => ValidatedData.success(cart)
    }

  private def validateHashCodeResult(
      cart: CartRecord,
      payload: CallbackPayload,
    )(implicit
      jetDirectConfig: JetdirectConfig,
    ): ValidatedData[Record] = {
    val expected =
      calculateJetdirectReturnHashCode(payload.orderNumber.toString, payload.amount.toString, payload.responseText)
    val actual = payload.jpReturnHash
    if (actual.contains(expected)) ValidatedData.success(cart)
    else {
      val msg =
        s"""[Jetdirect Code Result $actual] Mismatch in jetDirect_hash_code_result.
           |Actual: $actual, Expected: $expected""".stripMargin
      logger.error(msg)
      ValidatedData.failure(InvalidPaymentProcessorHashCodeResult(PaymentProcessor.Jetdirect, Some(actual)))
    }
  }

}
