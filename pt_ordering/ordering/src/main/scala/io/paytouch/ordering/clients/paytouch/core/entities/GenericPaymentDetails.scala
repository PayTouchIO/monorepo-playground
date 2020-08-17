package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.{ Currency, UUID }

import io.paytouch.ordering.clients.paytouch.core.entities.enums.{
  CardTransactionResultType,
  CardTransactionStatusType,
  CardType,
}
import io.paytouch.ordering.entities.ekashu.SuccessPayload
import io.paytouch.ordering.entities.enums.PaymentProcessor

final case class GenericPaymentDetails(
    amount: BigDecimal,
    currency: Currency,
    accountId: Option[String] = None,
    applicationId: Option[String] = None,
    authCode: Option[String] = None,
    maskPan: Option[String] = None,
    cardHash: Option[String] = None,
    cardReference: Option[String] = None,
    cardType: Option[CardType] = None,
    cardHolderName: Option[String] = None,
    terminalName: Option[String] = None,
    terminalId: Option[String] = None,
    transactionResult: Option[CardTransactionResultType] = None,
    transactionStatus: Option[CardTransactionStatusType] = None,
    transactionReference: Option[String] = None,
    last4Digits: Option[String] = None,
    paidInAmount: Option[BigDecimal] = None,
    paidOutAmount: Option[BigDecimal] = None,
    batchNumber: Option[Int] = None,
    merchantFee: Option[BigDecimal] = None,
    giftCardPassId: Option[UUID] = None,
    giftCardPassTransactionId: Option[UUID] = None,
    giftCardPassLookupId: Option[String] = None,
    isStandalone: Option[Boolean] = None,
    cashbackAmount: Option[BigDecimal] = None,
    customerId: Option[UUID] = None,
    tipAmount: BigDecimal,
    preauth: Boolean = false,
    terminalVerificationResult: Option[String] = None,
    applicationDedicatedFile: Option[String] = None,
    transactionStatusInfo: Option[String] = None,
    applicationLabel: Option[String] = None,
    entryMode: Option[String] = None, // Manual, Swipe, Chip, Contactless, Scan, Check Reader
    transactionNumber: Option[String] = None,
    gatewayTransactionReference: Option[String] = None,
    originalPayload: Option[SuccessPayload] = None,
  )
