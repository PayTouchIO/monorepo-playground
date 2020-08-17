package io.paytouch.ordering.errors

import java.util.{ Currency, UUID }

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.data.model.CartRecord
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.entities.stripe.Livemode

abstract class DataError extends BadRequest {
  override val values: Seq[AnyRef] = Seq.empty
  override val objectId: Option[UUID] = None
  override val field: Option[String] = None
}

final case class ArbitraryDataError(
    message: String,
    code: String,
    override val values: Seq[AnyRef],
    override val objectId: Option[UUID] = None,
    override val field: Option[String] = None,
  ) extends DataError

object AddressRequiredForDelivery {

  def apply(cardId: UUID): DataError =
    ArbitraryDataError(
      message = "Address is required to create a cart of delivery",
      code = "AddressRequiredForDelivery",
      values = Seq(cardId),
    )
}

object InvalidStoreLocationAssociation {

  def apply(locationId: UUID): DataError =
    ArbitraryDataError(
      message = "A location cannot have more than one store",
      code = "InvalidStoreLocationAssociation",
      values = Seq(locationId),
    )
}

object UrlSlugAlreadyTaken {

  def apply(urlSlug: String): DataError =
    ArbitraryDataError(message = "Url slug is already taken", code = "UrlSlugAlreadyTaken", values = Seq(urlSlug))
}

object ImmutableCart {

  def apply(cartIds: Seq[UUID]): DataError =
    ArbitraryDataError(message = s"Carts cannot be changed or deleted", code = "ImmutableCart", values = cartIds)

  def apply(cartId: UUID): DataError =
    ArbitraryDataError(
      message = s"Cart $cartId cannot be changed or deleted",
      code = "ImmutableCart",
      values = Seq(cartId),
    )
}

object InvalidModifierOptionIds {

  def apply(ids: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = "Modifier options do not belong to this product",
      code = "InvalidModifierOptionIds",
      values = ids,
    )
}

object DisabledModifierOptionIds {

  def apply(ids: Seq[UUID]): DataError =
    ArbitraryDataError(message = "Modifier option ids are disabled", code = "DisabledModifierOptionIds", values = ids)
}

object DisabledSetModifierOptionIds {

  def apply(ids: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = "Modifier option ids belong to a disabled modifier set",
      code = "DisabledSetModifierOptionIds",
      values = ids,
    )
}

object ImpossibleState {

  def apply(message: String): DataError =
    ArbitraryDataError(message = message, code = "ImpossibleState", values = Seq.empty)
}

object InvalidPaymentProcessorReference {
  val code = "InvalidPaymentProcessorReference"

  def apply(paymentProcessor: PaymentProcessor, maybeId: Option[UUID]): DataError =
    ArbitraryDataError(
      message = s"Invalid $paymentProcessor reference",
      objectId = maybeId,
      code = code,
      values = Seq.empty,
    )
}

object InvalidPaymentProcessorHashCodeResult {
  def apply(paymentProcessor: PaymentProcessor, hashCode: Option[String]): DataError =
    ArbitraryDataError(
      message =
        s"[$paymentProcessor Code Result $hashCode] Hash Code Result doesn't match our expectations. More information in the logs ",
      objectId = None,
      code = "InvalidPaymentProcessorHashCodeResult",
      values = Seq.empty,
    )
}

object MissingPaymentProcessorConfig {
  def apply(paymentProcessor: PaymentProcessor, merchantId: UUID): DataError =
    ArbitraryDataError(
      message = s"Couldn't find an $paymentProcessor configuration for merchant $merchantId",
      objectId = Some(merchantId),
      code = "MissingPaymentProcessorConfig",
      values = Seq.empty,
    )
}

object UnsupportedPaymentProcessorCurrency {
  def apply(
      paymentProcessor: PaymentProcessor,
      currency: Currency,
      merchantId: UUID,
    ): DataError =
    ArbitraryDataError(
      message = s"Processor $paymentProcessor does not support store currency $currency for merchant $merchantId",
      objectId = None,
      code = "UnsupportedPaymentProcessorCurrency",
      values = Seq.empty,
    )
}

object PaymentProcessorTotalMismatch {

  def apply(
      paymentProcessor: PaymentProcessor,
      incomingTotal: BigDecimal,
      objectId: UUID,
      expectedTotal: BigDecimal,
    ): DataError =
    ArbitraryDataError(
      message = s"total from $paymentProcessor is $incomingTotal, expected total is ${expectedTotal}",
      objectId = Some(objectId),
      code = "PaymentProcessorTotalMismatch",
      values = Seq.empty,
    )
}

object PaymentProcessorTipMismatch {

  def apply(
      paymentProcessor: PaymentProcessor,
      cart: CartRecord,
      incomingTip: Option[BigDecimal],
    ): DataError =
    ArbitraryDataError(
      message = s"tip from $paymentProcessor is $incomingTip, cart tip is ${cart.tipAmount}",
      objectId = Some(cart.id),
      code = "PaymentProcessorTipMismatch",
      values = Seq.empty,
    )
}

object PaymentProcessorMissingMandatoryField {

  def apply(paymentProcessor: PaymentProcessor, fieldName: String): DataError =
    ArbitraryDataError(
      message = s"$paymentProcessor: Missing mandatory field $fieldName",
      code = "PaymentProcessorMissingMandatoryField",
      values = Seq.empty,
    )
}

object PaymentProcessorUnparsableMandatoryField {

  def apply(
      paymentProcessor: PaymentProcessor,
      fieldName: String,
      value: Option[String],
      exception: Throwable,
    ): DataError =
    ArbitraryDataError(
      message = s"$paymentProcessor: Could not parse $fieldName value $value (exception: ${exception.getMessage})",
      code = "PaymentProcessorUnparsableMandatoryField",
      values = Seq.empty,
    )
}

object AddressTooFarForDelivery {

  def apply(
      cardId: UUID,
      currentDistance: BigDecimal,
      maxDistance: BigDecimal,
    ): DataError =
    ArbitraryDataError(
      message = s"Address too far for delivery (current: $currentDistance, max: $maxDistance)",
      code = "AddressTooFarForDelivery",
      values = Seq(cardId),
    )
}

object GiftCardPassIsUsedUp {
  def apply(onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw): DataError =
    ArbitraryDataError(
      message = s"Gift card with online code: ${onlineCode.value} is used up.",
      code = "GiftCardPassIsUsedUp",
      values = Seq.empty,
    )
}

object NegativeTip {

  def apply(tip: BigDecimal): DataError =
    ArbitraryDataError(message = s"Found tip of $tip. Tip cannot be negative", code = "NegativeTip", values = Seq.empty)
}

object CartTotalOutOfBounds {
  def apply(
      total: BigDecimal,
      min: BigDecimal,
      max: BigDecimal,
    ): DataError =
    ArbitraryDataError(
      message = s"Cart total $total should be within $min and $max.",
      code = "CartTotalOutOfBounds",
      values = Seq.empty,
    )
}

object NotInDevelopment {

  def apply(): DataError =
    ArbitraryDataError(
      message = s"This call is not available outside development",
      code = "NotInDevelopment",
      values = Seq.empty,
    )
}

object BundleMetadataForNonBundleProduct {

  def apply(productId: UUID): DataError =
    ArbitraryDataError(
      message = s"Found bundle options for a non bundle product $productId",
      code = "BundleMetadataForNonBundleProduct",
      values = Seq.empty,
    )
}

object MissingBundleMetadataForBundleProduct {

  def apply(productId: UUID): DataError =
    ArbitraryDataError(
      message = s"Missing bundle options for a bundle product $productId",
      code = "MissingBundleOptionsForBundleProduct",
      values = Seq.empty,
    )
}

object NotEnoughOptionsForBundleSet {
  def apply(bundleSetId: UUID): DataError =
    ArbitraryDataError(
      message = s"Not enough options for bundle set $bundleSetId",
      code = "NotEnoughOptionsForBundleSet",
      values = Seq.empty,
    )
}

object TooManyOptionsForBundleSet {
  def apply(bundleSetId: UUID): DataError =
    ArbitraryDataError(
      message = s"Too many options for bundle set $bundleSetId",
      code = "TooManyOptionsForBundleSet",
      values = Seq.empty,
    )
}

object DuplicatedBundleMetadataForBundleProduct {
  def apply(productId: UUID): DataError =
    ArbitraryDataError(
      message = s"Duplicate bundle metadata for product $productId",
      code = "DuplicatedBundleMetadataForBundleProduct",
      values = Seq.empty,
    )
}

object MissingBundleMetadataForBundleSet {
  def apply(bundleSetId: UUID): DataError =
    ArbitraryDataError(
      message = s"Missing metadata for bundle set $bundleSetId",
      code = "MissingBundleMetadataForBundleSet",
      values = Seq.empty,
    )
}

object MissingProductForBundleOption {
  def apply(productId: UUID, bundleOptionId: UUID): DataError =
    ArbitraryDataError(
      message = s"Missing product $productId for bundle option $bundleOptionId",
      code = "MissingProductForBundleOption",
      values = Seq.empty,
    )
}

object FailedToFetchAllProducts {
  def apply(requestedProductIds: Seq[UUID], fetchedProductIds: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = s"Requested $requestedProductIds but only got $fetchedProductIds",
      code = "FailedToFetchAllProducts",
      values = Seq.empty,
    )
}

object InvalidBundleOptionIdForBundleSet {
  def apply(
      bundleSetId: UUID,
      bundleOptionId: UUID,
      validOptionIds: Seq[UUID],
    ): DataError =
    ArbitraryDataError(
      message =
        s"Invalid option id $bundleOptionId for bundle set $bundleSetId. Valid ids are: ${validOptionIds.mkString(",")}",
      code = "InvalidBundleOptionIdForBundleSet",
      values = Seq.empty,
    )
}

object BundleOptionIdAndArticleIdMismatchForBundleSet {
  def apply(
      bundleSetId: UUID,
      bundleOption: BundleOption,
      givenArticleId: UUID,
    ): DataError =
    ArbitraryDataError(
      message =
        s"Unexpected article id $givenArticleId for ${bundleOption.id} in bundle set $bundleSetId. Expected: ${bundleOption.article.id}",
      code = "BundleOptionIdAndArticleIdMismatchForBundleSet",
      values = Seq.empty,
    )
}

object UnexpectedCaseWhileValidatingSequence {
  def apply(description: String): DataError =
    ArbitraryDataError(
      message = s"Didn't expect a None here when $description",
      code = "UnexpectedCaseWhileValidatingSequence",
      values = Seq.empty,
    )
}

object ProductOutOfStock {
  def apply(productId: UUID): DataError =
    ArbitraryDataError(message = "Product is out of stock", code = "ProductOutOfStock", values = Seq(productId))
}

object UnsupportedPaymentMethodType {
  def apply(paymentMethodType: PaymentMethodType): DataError =
    ArbitraryDataError(
      message = s"Payment method $paymentMethodType is not supported or not enabled for this store",
      code = "UnsupportedPaymentMethodType",
      values = Seq.empty,
    )
}

object MissingPaymentMethodType {
  def apply(): DataError =
    ArbitraryDataError(
      message = s"Payment method is required to checkout cart",
      code = "MissingPaymentMethodType",
      values = Seq.empty,
    )
}

object MissingCheckoutReturnUrl {

  def apply(): DataError =
    ArbitraryDataError(message = "Missing checkout return urls", code = "MissingCheckoutReturnUrl", values = Seq.empty)
}

object AlreadyPaidOrderItems {
  def apply(ids: Seq[UUID]): DataError =
    ArbitraryDataError(message = "Already paid order item ids", code = "AlreadyPaidOrderItems", values = ids)
}

// For v1 only. To be removed for payment intents v2.
object NotIncludedOrderItems {
  def apply(ids: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = "Not all order items were selected. Split payments are not supported right now.",
      code = "NotIncludedOrderItems",
      values = ids,
    )
}

final object UnhandledStripeWebhookType {
  def apply(eventType: String): DataError =
    ArbitraryDataError(
      message = s"Unhandled webhook type ${eventType}",
      code = "UnhandledStripeWebhookType",
      values = Seq.empty,
    )
}

final case class GiftCardPassByOnlineCodeNotFound(onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw)
    extends DataError {
  override val message = s"Can't find a gift card with given code ${onlineCode.value}"
  override val code = "GiftCardPassByOnlineCodeNotFound"
}

final case class GiftCardPassAlreadyCharged(onlineCode: io.paytouch.GiftCardPass.OnlineCode.Raw) extends DataError {
  override val message = s"Gift card with given code ${onlineCode.value} is already charged"
  override val code = "GiftCardPassAlreadyCharged"
}

case object GiftCardPassesNotAllFound extends DataError {
  override val message = "Not all gift card passes were found for order."
  override val code = "GiftCardPassesNotAllFound"
}

final case class InsufficientFunds(bulkFailure: Seq[GiftCardPassCharge.Failure]) extends DataError {
  override val message = s"One or more gift card passes did not have sufficient funds to cover the requested amount."
  override val code = "InsufficientFunds"
  override val values = bulkFailure
}
