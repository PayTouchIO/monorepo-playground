package io.paytouch.ordering.jetdirect

import java.util.{ Currency, UUID }

import cats.data.Validated.{ Invalid, Valid }
import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType
import io.paytouch.ordering.entities.enums.{ PaymentProcessor, PaymentProcessorCallbackStatus }
import io.paytouch.ordering.entities.jetdirect.{ CallbackPayload, JetdirectCallbackStatus }
import io.paytouch.ordering.errors.{ PaymentProcessorMissingMandatoryField, PaymentProcessorUnparsableMandatoryField }
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedOptData.ValidatedOptData

import scala.util.{ Failure, Success, Try }

trait JetdirectParser {
  def parseJetdirectEntity(implicit fields: Map[String, String]): ValidatedData[CallbackPayload] =
    parseData("responseText", JetdirectCallbackStatus.withNameInsensitive) match {
      case Valid(JetdirectCallbackStatus.Approved) => parseJetdirectEntityApproved(fields)
      case Valid(JetdirectCallbackStatus.Declined) => parseJetdirectEntityDeclined(fields)
      case Invalid(error)                          => ValidatedData.failure(error.head)
    }

  private def parseJetdirectEntityApproved(implicit fields: Map[String, String]): ValidatedData[CallbackPayload] = {
    val validJetdirectResponseText = parseData("responseText", identity)
    val validJetdirectAmount = parseData("amount", BigDecimal.apply)
    val validJetdirectCardScheme = parseData("card", CardType.withJetdirectName)
    val validJetdirectReference = parseData("order_number", UUID.fromString)
    val validJetdirectReturnHash = parseData("jp_return_hash", identity)
    val validJetdirectFeeAmount = parseOptData("feeAmount", BigDecimal.apply)
    val validJetdirectTipAmount = parseOptData("tipAmount", BigDecimal.apply)

    ValidatedData.combine(
      validJetdirectResponseText,
      validJetdirectAmount,
      validJetdirectCardScheme,
      validJetdirectReference,
      validJetdirectReturnHash,
      validJetdirectFeeAmount,
      validJetdirectTipAmount,
    ) {
      case (responseText, jetDirectAmount, cardScheme, reference, returnHash, feeAmount, tipAmount) =>
        buildPayload(
          JetdirectCallbackStatus.Approved,
          responseText,
          jetDirectAmount,
          Some(cardScheme),
          reference,
          returnHash,
          feeAmount,
          tipAmount,
        )
    }
  }

  private def parseJetdirectEntityDeclined(implicit fields: Map[String, String]): ValidatedData[CallbackPayload] = {
    val validJetdirectResponseText = parseData("responseText", identity)
    val validJetdirectAmount = parseData("amount", BigDecimal.apply)
    val validJetdirectCardScheme = parseOptData("card", CardType.withJetdirectName)
    val validJetdirectReference = parseData("order_number", UUID.fromString)
    val validJetdirectReturnHash = parseData("jp_return_hash", identity)
    val validJetdirectFeeAmount = parseOptData("feeAmount", BigDecimal.apply)
    val validJetdirectTipAmount = parseOptData("tipAmount", BigDecimal.apply)

    ValidatedData.combine(
      validJetdirectResponseText,
      validJetdirectAmount,
      validJetdirectCardScheme,
      validJetdirectReference,
      validJetdirectReturnHash,
      validJetdirectFeeAmount,
      validJetdirectTipAmount,
    ) {
      case (responseText, jetDirectAmount, cardScheme, reference, returnHash, feeAmount, tipAmount) =>
        buildPayload(
          JetdirectCallbackStatus.Declined,
          responseText,
          jetDirectAmount,
          cardScheme,
          reference,
          returnHash,
          feeAmount,
          tipAmount,
        )
    }
  }

  private def buildPayload(
      status: JetdirectCallbackStatus,
      responseText: String,
      jetDirectAmount: BigDecimal,
      cardScheme: Option[CardType],
      reference: UUID,
      returnHash: String,
      feeAmount: Option[BigDecimal],
      tipAmount: Option[BigDecimal],
    )(implicit
      fields: Map[String, String],
    ) =
    CallbackPayload(
      status = status.genericStatus,
      responseText = responseText,
      cid = fields.get("cid"),
      name = fields.get("name"),
      card = cardScheme,
      cardNum = fields.get("cardNum"),
      expandedCardNum = fields.get("expandedCardNum"),
      expDate = fields.get("expDate"),
      amount = jetDirectAmount,
      transId = fields.get("transId"),
      actCode = fields.get("actCode"),
      apprCode = fields.get("apprCode"),
      cvvMatch = fields.get("cvvMatch"),
      addressMatch = fields.get("addressMatch"),
      zipMatch = fields.get("zipMatch"),
      avsMatch = fields.get("avsMatch"),
      ccToken = fields.get("ccToken"),
      customerEmail = fields.get("customerEmail"),
      orderNumber = reference,
      jpReturnHash = returnHash,
      rrn = fields.get("rrn"),
      uniqueid = fields.get("uniqueid"),
      rawResponse = fields.get("rawResponse"),
      feeAmount = feeAmount,
      tipAmount = tipAmount,
    )

  private def parseData[T](
      fieldName: String,
      f: String => T,
    )(implicit
      fields: Map[String, String],
    ): ValidatedData[T] = {
    val maybeString = fields.get(fieldName)
    Try(maybeString.map(f)) match {
      case Success(Some(result)) => ValidatedData.success(result)
      case Success(None) =>
        ValidatedData.failure(PaymentProcessorMissingMandatoryField(PaymentProcessor.Jetdirect, fieldName))
      case Failure(exception) =>
        ValidatedData.failure(
          PaymentProcessorUnparsableMandatoryField(PaymentProcessor.Jetdirect, fieldName, maybeString, exception),
        )
    }
  }

  private def parseOptData[T](
      fieldName: String,
      f: String => T,
    )(implicit
      fields: Map[String, String],
    ): ValidatedOptData[T] =
    fields.get(fieldName) match {
      case Some("") => ValidatedData.success(None)
      case maybeString =>
        Try(maybeString.map(f)) match {
          case Success(r) => ValidatedData.success(r)
          case Failure(exception) =>
            ValidatedData.failure(
              PaymentProcessorUnparsableMandatoryField(PaymentProcessor.Jetdirect, fieldName, maybeString, exception),
            )
        }
    }
}
