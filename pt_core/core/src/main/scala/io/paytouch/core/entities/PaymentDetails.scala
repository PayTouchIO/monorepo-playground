package io.paytouch.core.entities

import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.enums.{ CardTransactionResultType, CardTransactionStatusType, CardType }
import io.paytouch.core.json.JsonSupport

final case class PaymentDetails(
    amount: Option[BigDecimal] = None,
    currency: Option[Currency] = None,
    authCode: Option[String] = None,
    maskPan: Option[String] = None,
    cardHash: Option[String] = None,
    cardReference: Option[String] = None,
    cardType: Option[CardType] = None,
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
    tipAmount: BigDecimal = 0,
    preauth: Boolean = false,
    terminalVerificationResult: Option[String] = None,
    applicationDedicatedFile: Option[String] = None,
    transactionStatusInfo: Option[String] = None,
    applicationLabel: Option[String] = None,
    applicationId: Option[String] = None,
    accountId: Option[String] = None,
    paymentAccountId: Option[String] = None,
    entryMode: Option[String] = None, // Manual, Swipe, Chip, Contactless, Scan, Check Reader
    transactionNumber: Option[String] = None,
    gatewayTransactionReference: Option[String] = None,
    cardHolderName: Option[String] = None,
    worldpay: Option[JsonSupport.JValue] = None,
    cryptogram: Option[String] = None,
    signatureRequired: Option[Boolean] = None,
    pinVerified: Option[Boolean] = None,
  )
