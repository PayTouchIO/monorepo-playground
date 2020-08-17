package io.paytouch.ordering.ekashu

import java.util.{ Currency, UUID }

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType
import io.paytouch.ordering.entities.ekashu.SuccessPayload
import io.paytouch.ordering.entities.enums.PaymentProcessor
import io.paytouch.ordering.errors.{ PaymentProcessorMissingMandatoryField, PaymentProcessorUnparsableMandatoryField }
import io.paytouch.ordering.utils.validation.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData
import io.paytouch.ordering.utils.validation.ValidatedOptData.ValidatedOptData

import scala.util.{ Failure, Success, Try }

trait EkashuParser {

  def parseEkashuEntity(fields: Map[String, String]): ValidatedData[SuccessPayload] = {
    implicit val f: Map[String, String] = fields

    val validEkashuAmount = parseData("ekashu_amount", BigDecimal.apply)
    val validEkashuCurrency = parseData("ekashu_currency", c => Currency.getInstance(c.toUpperCase))
    val validEkashuTransactionID = parseData("ekashu_transaction_id", identity)
    val validEkashuCardScheme = parseOptData("ekashu_card_scheme", CardType.withEkashuName)
    val validEkashuReference = parseOptData("ekashu_reference", UUID.fromString)

    ValidatedData.combine(
      validEkashuAmount,
      validEkashuCurrency,
      validEkashuTransactionID,
      validEkashuCardScheme,
      validEkashuReference,
    ) {
      case (ekashuAmount, ekashuCurrency, ekashuTransactionID, ekashuCardScheme, ekashuReference) =>
        SuccessPayload(
          ekashu3dSecureEci = fields.get("ekashu_3d_secure_eci"),
          ekashu3dSecureEnrolled = fields.get("ekashu_3d_secure_enrolled"),
          ekashu3dSecureIav = fields.get("ekashu_3d_secure_iav"),
          ekashu3dSecureResult = fields.get("ekashu_3d_secure_result"),
          ekashu3dSecureVerify = fields.get("ekashu_3d_secure_verify"),
          ekashu3dSecureXid = fields.get("ekashu_3d_secure_xid"),
          ekashuAmount = ekashuAmount,
          ekashuAmountFormat = fields.get("ekashu_amount_format"),
          ekashuAuthCode = fields.get("ekashu_auth_code"),
          ekashuAuthResult = fields.get("ekashu_auth_result"),
          ekashuAutoConfirm = fields.get("ekashu_auto_confirm"),
          ekashuCallbackFailureURL = fields.get("ekashu_callback_failure_url"),
          ekashuCallbackIncludePost = fields.get("ekashu_callback_include_post"),
          ekashuCallbackSuccessURL = fields.get("ekashu_callback_success_url"),
          ekashuCardAddress1 = fields.get("ekashu_card_address_1"),
          ekashuCardAddress2 = fields.get("ekashu_card_address_2"),
          ekashuCardAddressEditable = fields.get("ekashu_card_address_editable"),
          ekashuCardAddressRequired = fields.get("ekashu_card_address_required"),
          ekashuCardAddressResult = fields.get("ekashu_card_address_result"),
          ekashuCardAddressVerify = fields.get("ekashu_card_address_verify"),
          ekashuCardCity = fields.get("ekashu_card_city"),
          ekashuCardCountry = fields.get("ekashu_card_country"),
          ekashuCardEmailAddress = fields.get("ekashu_card_email_address"),
          ekashuCardEmailAddressMandatory = fields.get("ekashu_card_email_address_mandatory"),
          ekashuCardFirstName = fields.get("ekashu_card_first_name"),
          ekashuCardHash = fields.get("ekashu_card_hash"),
          ekashuCardIin = fields.get("ekashu_card_iin"),
          ekashuCardLastName = fields.get("ekashu_card_last_name"),
          ekashuCardPhoneNumber = fields.get("ekashu_card_phone_number"),
          ekashuCardPhoneNumberMandatory = fields.get("ekashu_card_phone_number_mandatory"),
          ekashuCardPhoneNumberType = fields.get("ekashu_card_phone_number_type"),
          ekashuCardReference = fields.get("ekashu_card_reference"),
          ekashuCardScheme = ekashuCardScheme,
          ekashuCardState = fields.get("ekashu_card_state"),
          ekashuCardTitle = fields.get("ekashu_card_title"),
          ekashuCardTitleMandatory = fields.get("ekashu_card_title_mandatory"),
          ekashuCardZipCode = fields.get("ekashu_card_zip_code"),
          ekashuCardZipCodeResult = fields.get("ekashu_card_zip_code_result"),
          ekashuCardZipCodeVerify = fields.get("ekashu_card_zip_code_verify"),
          ekashuCurrency = ekashuCurrency,
          ekashuDateTimeLocal = fields.get("ekashu_date_time_local"),
          ekashuDateTimeLocalFmt = fields.get("ekashu_date_time_local_fmt"),
          ekashuDateTimeUtc = fields.get("ekashu_date_time_utc"),
          ekashuDateTimeUtcFmt = fields.get("ekashu_date_time_utc_fmt"),
          ekashuDeliveryAddress1 = fields.get("ekashu_delivery_address_1"),
          ekashuDeliveryAddress2 = fields.get("ekashu_delivery_address_2"),
          ekashuDeliveryAddressEditable = fields.get("ekashu_delivery_address_editable"),
          ekashuDeliveryAddressIsCardAddress = fields.get("ekashu_delivery_address_is_card_address"),
          ekashuDeliveryAddressRequired = fields.get("ekashu_delivery_address_required"),
          ekashuDeliveryCity = fields.get("ekashu_delivery_city"),
          ekashuDeliveryCountry = fields.get("ekashu_delivery_country"),
          ekashuDeliveryEmailAddress = fields.get("ekashu_delivery_email_address"),
          ekashuDeliveryEmailAddressMandatory = fields.get("ekashu_delivery_email_address_mandatory"),
          ekashuDeliveryFirstName = fields.get("ekashu_delivery_first_name"),
          ekashuDeliveryLastName = fields.get("ekashu_delivery_last_name"),
          ekashuDeliveryPhoneNumber = fields.get("ekashu_delivery_phone_number"),
          ekashuDeliveryPhoneNumberMandatory = fields.get("ekashu_delivery_phone_number_mandatory"),
          ekashuDeliveryPhoneNumberType = fields.get("ekashu_delivery_phone_number_type"),
          ekashuDeliveryState = fields.get("ekashu_delivery_state"),
          ekashuDeliveryTitle = fields.get("ekashu_delivery_title"),
          ekashuDeliveryTitleMandatory = fields.get("ekashu_delivery_title_mandatory"),
          ekashuDeliveryZipCode = fields.get("ekashu_delivery_zip_code"),
          ekashuDescription = fields.get("ekashu_description"),
          ekashuDevice = fields.get("ekashu_device"),
          ekashuDuplicateCheck = fields.get("ekashu_duplicate_check"),
          ekashuDuplicateMinutes = fields.get("ekashu_duplicate_minutes"),
          ekashuExpiresEndMonth = fields.get("ekashu_expires_end_month"),
          ekashuExpiresEndYear = fields.get("ekashu_expires_end_year"),
          ekashuFailureReturnText = fields.get("ekashu_failure_return_text"),
          ekashuFailureURL = fields.get("ekashu_failure_url"),
          ekashuHashCode = fields.get("ekashu_hash_code"),
          ekashuHashCodeFormat = fields.get("ekashu_hash_code_format"),
          ekashuHashCodeResult = fields.get("ekashu_hash_code_result"),
          ekashuHashCodeResultFormat = fields.get("ekashu_hash_code_result_format"),
          ekashuHashCodeResultType = fields.get("ekashu_hash_code_result_type"),
          ekashuHashCodeResultVersion = fields.get("ekashu_hash_code_result_version"),
          ekashuHashCodeType = fields.get("ekashu_hash_code_type"),
          ekashuHashCodeVersion = fields.get("ekashu_hash_code_version"),
          ekashuIncludePost = fields.get("ekashu_include_post"),
          ekashuInvoiceAddress1 = fields.get("ekashu_invoice_address_1"),
          ekashuInvoiceAddress2 = fields.get("ekashu_invoice_address_2"),
          ekashuInvoiceAddressEditable = fields.get("ekashu_invoice_address_editable"),
          ekashuInvoiceAddressIsCardAddress = fields.get("ekashu_invoice_address_is_card_address"),
          ekashuInvoiceAddressRequired = fields.get("ekashu_invoice_address_required"),
          ekashuInvoiceCity = fields.get("ekashu_invoice_city"),
          ekashuInvoiceCountry = fields.get("ekashu_invoice_country"),
          ekashuInvoiceEmailAddress = fields.get("ekashu_invoice_email_address"),
          ekashuInvoiceEmailAddressMandatory = fields.get("ekashu_invoice_email_address_mandatory"),
          ekashuInvoiceFirstName = fields.get("ekashu_invoice_first_name"),
          ekashuInvoiceLastName = fields.get("ekashu_invoice_last_name"),
          ekashuInvoicePhoneNumber = fields.get("ekashu_invoice_phone_number"),
          ekashuInvoicePhoneNumberMandatory = fields.get("ekashu_invoice_phone_number_mandatory"),
          ekashuInvoicePhoneNumberType = fields.get("ekashu_invoice_phone_number_type"),
          ekashuInvoiceState = fields.get("ekashu_invoice_state"),
          ekashuInvoiceTitle = fields.get("ekashu_invoice_title"),
          ekashuInvoiceTitleMandatory = fields.get("ekashu_invoice_title_mandatory"),
          ekashuInvoiceZipCode = fields.get("ekashu_invoice_zip_code"),
          ekashuIssueNumber = fields.get("ekashu_issue_number"),
          ekashuLocale = fields.get("ekashu_locale"),
          ekashuMaskedCardNumber = fields.get("ekashu_masked_card_number"),
          ekashuMerchantCategoryCode = fields.get("ekashu_merchant_category_code"),
          ekashuPaymentMethod = fields.get("ekashu_payment_method"),
          ekashuPaymentMethods = fields.get("ekashu_payment_methods"),
          ekashuPaypalToken = fields.get("ekashu_paypal_token"),
          ekashuPaypalTransactionID = fields.get("ekashu_paypal_transaction_id"),
          ekashuProducts = fields.get("ekashu_products"),
          ekashuRecipientAccountNumber = fields.get("ekashu_recipient_account_number"),
          ekashuRecipientAccountNumberIsCardNumber = fields.get("ekashu_recipient_account_number_is_card_number"),
          ekashuRecipientDateOfBirthDay = fields.get("ekashu_recipient_date_of_birth_day"),
          ekashuRecipientDateOfBirthMonth = fields.get("ekashu_recipient_date_of_birth_month"),
          ekashuRecipientDateOfBirthYear = fields.get("ekashu_recipient_date_of_birth_year"),
          ekashuRecipientFields = fields.get("ekashu_recipient_fields"),
          ekashuRecipientLastName = fields.get("ekashu_recipient_last_name"),
          ekashuRecipientZipCode = fields.get("ekashu_recipient_zip_code"),
          ekashuReference = ekashuReference,
          ekashuRequestType = fields.get("ekashu_request_type"),
          ekashuReturnText = fields.get("ekashu_return_text"),
          ekashuReturnURL = fields.get("ekashu_return_url"),
          ekashuSellerAddress = fields.get("ekashu_seller_address"),
          ekashuSellerEmailAddress = fields.get("ekashu_seller_email_address"),
          ekashuSellerID = fields.get("ekashu_seller_id"),
          ekashuSellerKey = fields.get("ekashu_seller_key"),
          ekashuSellerName = fields.get("ekashu_seller_name"),
          ekashuShortcutIcon = fields.get("ekashu_shortcut_icon"),
          ekashuStyleSheet = fields.get("ekashu_style_sheet"),
          ekashuSuccessURL = fields.get("ekashu_success_url"),
          ekashuTitle = fields.get("ekashu_title"),
          ekashuTransactionID = ekashuTransactionID,
          ekashuValidFromMonth = fields.get("ekashu_valid_from_month"),
          ekashuValidFromYear = fields.get("ekashu_valid_from_year"),
          ekashuVerificationValueMask = fields.get("ekashu_verification_value_mask"),
          ekashuVerificationValueResult = fields.get("ekashu_verification_value_result"),
          ekashuVerificationValueVerify = fields.get("ekashu_verification_value_verify"),
          ekashuViewport = fields.get("ekashu_viewport"),
        )
    }
  }

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
        ValidatedData.failure(PaymentProcessorMissingMandatoryField(PaymentProcessor.Ekashu, fieldName))
      case Failure(exception) =>
        ValidatedData.failure(
          PaymentProcessorUnparsableMandatoryField(PaymentProcessor.Ekashu, fieldName, maybeString, exception),
        )
    }
  }

  private def parseOptData[T](
      fieldName: String,
      f: String => T,
    )(implicit
      fields: Map[String, String],
    ): ValidatedOptData[T] = {
    val maybeString = fields.get(fieldName)
    Try(maybeString.map(f)) match {
      case Success(r) => ValidatedData.success(r)
      case Failure(exception) =>
        ValidatedData.failure(
          PaymentProcessorUnparsableMandatoryField(PaymentProcessor.Ekashu, fieldName, maybeString, exception),
        )
    }
  }
}
