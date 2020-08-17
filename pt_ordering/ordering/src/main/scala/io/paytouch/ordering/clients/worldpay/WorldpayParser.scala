package io.paytouch.ordering.clients.worldpay

import cats.data.Validated.{ Invalid, Valid }
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.clients.worldpay.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.errors.{ PaymentProcessorMissingMandatoryField, PaymentProcessorUnparsableMandatoryField }
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

import scala.util.{ Failure, Success, Try }
import scala.xml.Node

trait WorldpayParser extends LazyLogging {
  def parseTransactionQueryResponse(elem: Node): WorldpayResponse = {
    implicit val e: Node = elem

    val validAccountId = parseData("AccountID", identity)
    val validApplicationId = parseData("ApplicationID", identity)
    val validApprovalNumber = parseData("ApprovalNumber", identity)
    val validApprovedAmount = parseData("ApprovedAmount", BigDecimal.apply)
    val validMaskedCardNumber = parseData("CardNumberMasked", parseCardNumberMasked)
    val validCardType = parseData("CardLogo", CardType.withWorldpayName)
    val validCardHolderName = parseData("Name", identity)
    val validTerminalId = parseData("TerminalID", identity)
    val validTransactionId = parseData("TransactionID", identity)
    val validTransactionSetupId = parseData("TransactionSetupID", identity)
    val validHostResponseCode = parseData("HostResponseCode", identity)

    val result = ValidatedData.combine(
      validAccountId,
      validApplicationId,
      validApprovalNumber,
      validApprovedAmount,
      validMaskedCardNumber,
      validCardType,
      validCardHolderName,
      validTerminalId,
      validTransactionId,
      validTransactionSetupId,
      validHostResponseCode,
    ) {
      case (
            accountId,
            applicationId,
            approvalNumber,
            approvedAmount,
            maskedCardNumber,
            cardType,
            cardHolderName,
            terminalId,
            transactionId,
            transactionSetupId,
            hostResponseCode,
          ) =>
        TransactionQueryResponse(
          accountId = accountId,
          applicationId = applicationId,
          approvalNumber = approvalNumber,
          approvedAmount = approvedAmount,
          maskedCardNumber = maskedCardNumber,
          cardType = cardType,
          cardHolderName = cardHolderName,
          terminalId = terminalId,
          transactionId = transactionId,
          transactionSetupId = transactionSetupId,
          hostResponseCode = hostResponseCode,
        )
    }

    result match {
      case Valid(response) => response
      case Invalid(error) =>
        logger.error(s"Error parsing worldpay response $error response = $elem")
        ErrorResponse(ResponseCode.ResponseParseError)
    }
  }

  private def parseData[T](fieldName: String, f: String => T)(implicit elem: Node): ValidatedData[T] = {
    val string = (elem \ fieldName).text

    // Convert empty strings to None, otherwise to Some
    val maybeString = Option(string).filter(_.trim.nonEmpty)

    Try(maybeString.map(f)) match {
      case Success(Some(result)) => ValidatedData.success(result)
      case Success(None) =>
        ValidatedData.failure(PaymentProcessorMissingMandatoryField(PaymentProcessor.Worldpay, fieldName))
      case Failure(exception) =>
        ValidatedData.failure(
          PaymentProcessorUnparsableMandatoryField(PaymentProcessor.Worldpay, fieldName, maybeString, exception),
        )
    }
  }

  // Make masked card number format consistent with other payment providers
  private def parseCardNumberMasked(value: String) =
    value.replace("-", "").replace("x", "*")
}
