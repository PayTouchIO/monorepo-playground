package io.paytouch.core.errors

import java.util.UUID
import java.time.ZonedDateTime

import enumeratum._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core._
import io.paytouch.core.clients.stripe.entities.StripeError
import io.paytouch.core.data._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.reports.data.model.ExportRecord
import io.paytouch.core.services._

abstract class DataError extends BadRequest {
  override def values: Seq[AnyRef] = Seq.empty
  override def objectId: Option[UUID] = None
  override def field: Option[String] = None
}

final case class ArbitraryDataError(
    override val message: String,
    override val code: String,
    override val values: Seq[AnyRef],
    override val objectId: Option[UUID] = None,
    override val field: Option[String] = None,
  ) extends DataError

object InvalidVariantOptionsInProductUpsertion {
  def apply(ids: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = "Variant products should cover every provided variant option",
      code = "InvalidVariantOptions",
      values = ids,
    )
}

object InvalidVariantSelectionsInProductUpsertion {
  def apply(missing: Seq[Seq[UUID]]): DataError =
    ArbitraryDataError(
      message = "Variant products selections needs to map the id provided in variants",
      code = "InvalidVariantSelections",
      values = missing,
    )
}

object InvalidSimpleToVariantTypeChange {
  def apply(): DataError =
    ArbitraryDataError(
      message = "Can't change a simple product to a template with variants",
      code = "InvalidSimpleToVariantTypeChange",
      values = Seq.empty,
    )
}

object InvalidProductTypeWithVariantsChange {
  def apply(): DataError =
    ArbitraryDataError(
      message = "A non-template product cannot have variants",
      code = "InvalidProductTypeWithVariantsChange",
      values = Seq.empty,
    )
}

object InvalidProductParentId {
  def apply(childrenIds: Seq[UUID], parentId: UUID): DataError =
    ArbitraryDataError(
      message = s"Invalid product parent $parentId for products ${childrenIds.mkString(", ")}",
      code = "InvalidProductParentId",
      values = Seq(parentId),
    )
}

object InvalidModifierSetIdPerModifierOptions {
  def apply(modifierOptionIds: Seq[UUID], modifierSetId: UUID): DataError =
    ArbitraryDataError(
      message = s"Invalid modifier set $modifierSetId for modifier options ${modifierOptionIds.mkString(", ")}",
      code = "InvalidModifierSetIdPerModifierOptions",
      values = Seq(modifierSetId),
    )
}

object InvalidVariantOptionIdsPerProduct {
  def apply(variantOptionIds: Seq[UUID], productId: UUID): DataError =
    ArbitraryDataError(
      message = s"Variant option ids not valid for product $productId",
      code = "InvalidVariantOptionIdsPerProduct",
      values = Seq("product" -> productId, "variant options" -> variantOptionIds),
    )
}

object InvalidVariantOptionAndOptionTypePerProduct {
  def apply(
      variantOptionIds: Seq[UUID],
      variantOptionTypeIds: Seq[UUID],
      productId: UUID,
    ): DataError =
    ArbitraryDataError(
      message = s"Variant option - variant option types do not match for product $productId",
      code = "InvalidVariantOptionAndOptionTypePerProduct",
      values = Seq(
        "product" -> productId,
        "variant options" -> variantOptionIds,
        "variant option types" -> variantOptionTypeIds,
      ),
    )
}

object InvalidTaxRateLocationAssociation {
  def apply(): DataError =
    ArbitraryDataError(
      message = s"Some or all the tax rate location associations are not valid.",
      code = "InvalidTaxRateLocationAssociation",
      values = Seq.empty,
    )
}

object InvalidSku {
  def apply(productId: UUID, sku: String): DataError =
    ArbitraryDataError(
      objectId = Some(productId),
      message = s"SKU is invalid. SKU must not contain spaces.",
      code = "InvalidSku",
      values = Seq(sku),
    )
}

object InvalidUpcs {
  def apply(upcs: Seq[String]): DataError =
    ArbitraryDataError(
      message = s"Some UPCs are invalid. UPCs must not contain spaces.",
      code = "InvalidUpcs",
      values = upcs,
    )

  def apply(invalidUpcs: Seq[String], upcsMap: Map[UUID, String]): Nel[DataError] = {
    val generalError = apply(invalidUpcs)
    val invalidUpcsMap = upcsMap.filter { case (_, v) => invalidUpcs.contains(v) }

    Nel.fromListUnsafe {
      generalError :: invalidUpcsMap.toList.map {
        case (productId, upc) =>
          ArbitraryDataError(
            objectId = Some(productId),
            message = s"Invalid UPC '$upc'. UPCs must not contain spaces.",
            code = "InvalidUpc",
            values = Seq(upc),
            field = Some("upc"),
          )
      }
    }
  }
}

object AlreadyTakenUpcs {
  def apply(upcs: Seq[String]): DataError =
    ArbitraryDataError(
      message = s"Some UPCs are already taken. UPCs must be unique.",
      code = "AlreadyTakenUpcs",
      values = upcs,
    )

  def apply(invalidUpcs: Seq[String], upcsMap: Map[UUID, String]): Nel[DataError] = {
    val generalError = apply(invalidUpcs)
    val invalidUpcsMap = upcsMap.filter { case (_, v) => invalidUpcs.contains(v) }

    Nel.fromListUnsafe {
      generalError :: invalidUpcsMap.toList.map {
        case (productId, upc) =>
          ArbitraryDataError(
            objectId = Some(productId),
            message = s"UPC '$upc' is already taken.",
            code = "AlreadyTakenUpc",
            values = Seq(upc),
            field = Some("upc"),
          )
      }
    }
  }
}

object DuplicatedUpcs {
  def apply(upcs: Seq[String]): DataError =
    ArbitraryDataError(
      message = s"Upcs must be unique. Some upcs are duplicated.",
      code = "DuplicatedUpcs",
      values = upcs,
    )

  def apply(invalidUpcs: Seq[String], upcsMap: Map[UUID, String]): Nel[DataError] = {
    val generalError = apply(invalidUpcs)
    val invalidUpcsMap = upcsMap.filter { case (_, v) => invalidUpcs.contains(v) }

    Nel.fromListUnsafe {
      generalError +: invalidUpcsMap.toList.map {
        case (productId, upc) =>
          ArbitraryDataError(
            objectId = Some(productId),
            message = s"Upcs '$upc' is duplicated.",
            code = "DuplicatedUpc",
            values = Seq(upc),
            field = Some("upc"),
          )
      }
    }
  }
}

object AlreadyTakenName {
  def apply(productId: UUID, name: String): DataError =
    ArbitraryDataError(
      objectId = Some(productId),
      message = s"Names must be unique. This name is already taken.",
      code = "AlreadyTakenName",
      values = Seq(name),
    )
}

object UnparsableImage {
  def apply(): DataError =
    ArbitraryDataError(message = s"Unparsable image from provided file.", code = "UnparsableImage", values = Seq.empty)
}

object InvalidImageUploadAssociation {
  def apply(ids: Seq[UUID], expectedType: ImageUploadType): DataError =
    ArbitraryDataError(
      message = s"Impossible to associate image uploads $ids as they are not of type ${expectedType.entryName}",
      code = "InvalidImageUploadAssociation",
      values = ids,
    )
}

object InvalidProductLocationAssociation {
  final case class ProductLocationRep(productId: UUID, locationId: UUID)

  def apply(relationIds: Seq[(UUID, UUID)]): DataError =
    ArbitraryDataError(
      message = s"Invalid product location association ${relationIds.mkString(", ")}",
      code = "InvalidProductLocationAssociation",
      values = relationIds.map { case (prd, loc) => ProductLocationRep(prd, loc) },
    )
}

object InvalidProductStockAssociation {
  def apply(stockIds: Seq[UUID], productId: UUID): DataError =
    ArbitraryDataError(
      message = s"Invalid stocks ${stockIds.mkString(", ")} for product $productId",
      code = "InvalidProductStockAssociation",
      values = stockIds,
    )
}

object InvalidOrderOrderItemsAssociation {
  def apply(orderItemIds: Seq[UUID], orderId: UUID): DataError =
    ArbitraryDataError(
      message = s"Invalid order items ${orderItemIds.mkString(", ")} for order $orderId",
      code = "InvalidOrderOrderItemsAssociation",
      values = orderItemIds,
    )
}

object InvalidTicketOrderItemsAssociation {
  def apply(): DataError =
    ArbitraryDataError(
      message = "A ticket must be always associated to at least one order item. Please provide at least one.",
      code = "InvalidTicketOrderItemsAssociation",
      values = Seq.empty,
    )
}

object InvalidOrderLocationAssociation {
  def apply(orderId: UUID, locationId: UUID): DataError =
    ArbitraryDataError(
      message = s"Order $orderId does not belong to location $locationId",
      code = "InvalidOrderLocationAssociation",
      values = Seq.empty,
    )
}

object InvalidPusherChannel {
  def apply(channelName: String, reason: String): DataError =
    ArbitraryDataError(
      message = s"Invalid channel $channelName: $reason",
      code = "InvalidPusherChannel",
      values = Seq.empty,
    )
}

object InvalidPusherSocketId {
  def apply(socketId: String): DataError =
    ArbitraryDataError(message = s"Invalid socket id $socketId", code = "InvalidPusherSocketId", values = Seq.empty)
}

object EmailAlreadyInUse {
  def apply(): DataError =
    ArbitraryDataError(
      message = s"Email is already in use by another account",
      code = "EmailAlreadyInUse",
      values = Seq.empty,
    )
}

object Auth0UserAlreadyInUse {
  def apply(): DataError =
    ArbitraryDataError(
      message = s"User already exists",
      code = "UserAlreadyExists",
      values = Seq.empty,
    )
}

object MerchantIdAlreadyInUse {
  def apply(id: UUID): DataError =
    ArbitraryDataError(message = s"Merchant id is already in use", code = "MerchantIdAlreadyInUse", values = Seq(id))
}

object TargetLocationDoesNotBelongToSameMerchant {
  def apply(merchantId: UUID): DataError =
    ArbitraryDataError(
      message = "Target location does not belong to the same merchant.",
      code = "TargetLocationDoesNotBelongToSameMerchant",
      values = Seq(merchantId),
    )
}

object PinAlreadyInUse {
  def apply(): DataError =
    ArbitraryDataError(
      message = s"Pin is already in use by another account",
      code = "PinAlreadyInUse",
      values = Seq.empty,
    )
}

object ReceivingObjectIdWithoutReceivingObjectType {
  def apply(receivingObjectId: UUID): DataError =
    ArbitraryDataError(
      message = s"Receiving object type is required when passing a receiving object id",
      code = "ReceivingObjectIdWithoutReceivingObjectType",
      values = Seq(receivingObjectId),
    )
}

object InvalidPaymentTransactionIdForOrderId {
  def apply(paymentTransactionId: UUID, orderId: UUID): DataError =
    ArbitraryDataError(
      message = s"Order $orderId does not have a payment transaction with id $paymentTransactionId",
      code = "InvalidPaymentTransactionIdForOrderId",
      values = Seq("paymentTransaction" -> paymentTransactionId, "order" -> orderId),
    )
}

object InvalidOrderSendReceipt {
  def apply(orderId: UUID): DataError =
    ArbitraryDataError(
      message = s"Order $orderId is not associated to a location. Cannot send receipt",
      code = "InvalidOrderForSendReceipt",
      values = Seq("order" -> orderId),
    )
}

object AlreadySyncedInventoryCount {
  def apply(inventoryCountId: UUID): DataError =
    ArbitraryDataError(
      message = s"Inventory count $inventoryCountId has already been synced and can't be synced again.",
      code = "AlreadySyncedInventoryCount",
      values = Seq.empty,
    )
}

object AlreadySentPurchaseOrder {
  def apply(purchaseOrderId: UUID): DataError =
    ArbitraryDataError(
      message = s"Purchase order $purchaseOrderId has already been sent and can't be edited anymore.",
      code = "AlreadySentPurchaseOrder",
      values = Seq.empty,
    )
}

object AlreadySyncedReceivingOrder {
  def apply(receivingOrderId: UUID): DataError =
    ArbitraryDataError(
      message = s"Receiving order $receivingOrderId has already been synced and can't be synced again.",
      code = "AlreadySyncedReceivingOrder",
      values = Seq.empty,
    )
}

object AlreadySyncedReceivingOrderInvalidUpdate {
  def apply(receivingOrderId: UUID): DataError =
    ArbitraryDataError(
      message = s"Receiving order $receivingOrderId has already been synced and can't update fields in input.",
      code = "AlreadySyncedReceivingOrder",
      values = Seq.empty,
    )
}

object AlreadySyncedReceivingOrderInvalidDeletion {
  def apply(receivingOrderIds: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = s"Receiving orders have already been synced and can't be deleted.",
      code = "AlreadySyncedReceivingOrderInvalidDeletion",
      values = receivingOrderIds,
    )
}

object AlreadySentPurchaseOrderInvalidDeletion {
  def apply(purchaseOrderIds: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = s"Purchase orders have already been sent and can't be deleted.",
      code = "AlreadySentPurchaseOrderInvalidDeletion",
      values = purchaseOrderIds,
    )
}

object AlreadySyncedReturnOrder {
  def apply(returnOrderId: UUID): DataError =
    ArbitraryDataError(
      message = s"Return order $returnOrderId has already been synced and can't be synced again.",
      code = "AlreadySyncedReturnOrder",
      values = Seq.empty,
    )
}

object InvalidEmail {
  def apply(email: String): DataError =
    ArbitraryDataError(message = s"$email is not a valid email.", code = "InvalidEmail", values = Seq.empty)
}

object InvalidPassword {
  def apply(): DataError =
    ArbitraryDataError(
      message = "The password does not meet requirements. It should contain at least 8 characters no spaces.",
      code = "InvalidPassword",
      values = Seq.empty,
    )
}

object NoPaymentTransactionsForOrderId {
  def apply(orderId: UUID): DataError =
    ArbitraryDataError(
      message = s"Order $orderId has no payment transactions",
      code = "NoPaymentTransactionsForOrderId",
      values = Seq.empty,
    )
}

case object NotPaymentTransaction {
  def apply(transactionId: UUID, transactionType: TransactionType): DataError =
    ArbitraryDataError(
      message = s"Transaction $transactionId is not of type ${TransactionType.Payment.entryName}",
      code = productPrefix,
      values = Seq(transactionType),
    )
}

case object TransactionNotApproved {
  def apply(transactionId: UUID, transactionResult: Option[CardTransactionResultType]): DataError =
    ArbitraryDataError(
      message = s"Transaction $transactionId not approved",
      code = productPrefix,
      values = Seq(transactionResult),
    )
}

object InvalidScopeProduct {
  def apply(expectedScope: ArticleScope, scopeProducts: Map[ArticleScope, Seq[UUID]]): DataError =
    ArbitraryDataError(
      message = s"Products needs to be of scope $expectedScope",
      code = "InvalidScopeProduct",
      values = scopeProducts.toSeq,
    )
}

object ExportDownloadMissingBaseUrl {
  def apply(export: ExportRecord): DataError =
    ArbitraryDataError(
      message = s"Cannot download export as base url is missing. Is the export completed? (status is ${export.status})",
      code = "ExportDownloadMissingBaseUrl",
      values = Seq(export.id),
    )
}

object ExceededIntervalsInTimeRange {
  def apply(intervalsInRange: Long, maxUnit: Int): DataError =
    ArbitraryDataError(
      message = s"Too many intervals in time range (received $intervalsInRange, but maximum is $maxUnit)",
      code = "ExceededIntervalsInTimeRange",
      values = Seq.empty,
    )
}

object GiftCardUniquePerMerchant {
  def apply(giftCardIds: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = s"A merchant can have only up to one gift card. Existing gift card id: ${giftCardIds.mkString(", ")}",
      code = "GiftCardUniquePerMerchant",
      values = Seq.empty,
    )
}

object GiftCardPassNotEnoughBalance {
  def apply(amount: BigDecimal, giftCardPass: GiftCardPassRecord): DataError =
    ArbitraryDataError(
      message =
        s"[Gift Card ${giftCardPass.id}] Not enough balance to cover the requested amount. Current balance: ${giftCardPass.balanceAmount}; Amount: $amount",
      code = "GiftCardPassNotEnoughBalance",
      values = Seq.empty,
    )
}

object GiftCardPassWithoutCustomerId {
  def apply(): DataError =
    ArbitraryDataError(
      message = s"No Customer id found for the order. Cannot create a gift card pass without a customer id.",
      code = "GiftCardPassWithoutCustomerId",
      values = Seq.empty,
    )
}

object GiftCardPassWithoutPrice {
  def apply(orderItemIds: Seq[UUID]): DataError =
    ArbitraryDataError(
      message =
        s"No price amount found for order items ${orderItemIds.mkString(", ")}. Cannot create a gift card pass from an item associated to a gift card without a price amount.",
      code = "GiftCardPassWithoutPrice",
      values = orderItemIds,
    )
}

object GiftCardPassWithoutGiftCard {
  def apply(): DataError =
    ArbitraryDataError(
      message = s"No gift card found. Cannot create a gift card pass without a gift card.",
      code = "GiftCardPassWithoutGiftCard",
      values = Seq.empty,
    )
}

object GiftCardPassesNotAllFound {
  def apply(orderId: OrderId): DataError =
    ArbitraryDataError(
      message = s"Not all gift card passes were found for order: ${orderId.value}",
      code = "GiftCardPassesNotAllFound",
      values = Seq.empty,
    )
}

object InsufficientFunds {
  def apply(orderId: OrderId, bulkFailure: Seq[GiftCardPassCharge.Failure]): DataError =
    ArbitraryDataError(
      message =
        s"One or more gift card passes for order: ${orderId.value} did not have sufficient funds to cover the requested amount.",
      code = "InsufficientFunds",
      values = bulkFailure,
    )
}

object OrderSyncMissingOptionName {
  def apply(upsertion: OrderItemVariantOptionUpsertion): DataError =
    ArbitraryDataError(
      message = s"Missing option name in variant upsertion: trying to recovering it from the db.",
      code = "OrderSyncMissingOptionName",
      values = Seq.empty,
    )
}

object OrderSyncMissingOptionTypeName {
  def apply(upsertion: OrderItemVariantOptionUpsertion): DataError =
    ArbitraryDataError(
      message = s"Missing option type name in variant upsertion: trying to recovering it from the db.",
      code = "OrderSyncMissingOptionTypeName",
      values = Seq.empty,
    )
}

object StockProductLocationDuplication {
  def apply(rel: Seq[(UUID, UUID)]): DataError =
    ArbitraryDataError(
      message = s"Bulk update not possible as product-location relation is duplicated.",
      code = "StockProductLocationDuplication",
      values = rel.map { case (pId, lId) => s"product id $pId <-> location id $lId" },
    )
}

object PaymentTransactionExpired {
  def apply(paymentTransactionId: UUID): DataError =
    ArbitraryDataError(
      message = s"Payment transaction $paymentTransactionId expired",
      code = "PaymentTransactionExpired",
      values = Seq.empty,
    )
}

object MerchantAlreadyInRequestedMode {
  def apply(merchantId: UUID): DataError =
    ArbitraryDataError(
      message = s"Merchant $merchantId is already in production mode",
      code = "MerchantAlreadyInProduction",
      values = Seq.empty,
    )
}

object SwitchToDemoModeUnsupported {
  def apply(): DataError =
    ArbitraryDataError(
      message = s"Switching merchant to demo mode is not supported.",
      code = "SwitchToDemoUnsupported",
      values = Seq.empty,
    )
}

object FirstLocationMissing {
  def apply(merchantId: UUID): DataError =
    ArbitraryDataError(
      message = s"Can't create a new merchant with no locations. Merchant $merchantId has zero locations.",
      code = "FirstLocationMissing",
      values = Seq.empty,
    )
}

object FirstOwnerMissing {
  def apply(merchantId: UUID): DataError =
    ArbitraryDataError(
      message = s"Can't create a new merchant with no owners. Merchant $merchantId has zero owners.",
      code = "FirstOwnerMissing",
      values = Seq.empty,
    )
}

object SampleDataForDemoModeOnly {
  def apply(merchantId: UUID, mode: MerchantMode): DataError =
    ArbitraryDataError(
      message =
        s"Sample data is available only for merchants in mode 'demo'. Merchant $merchantId has mode ${mode.entryName}",
      code = "SampleDataForDemoModeOnly",
      values = Seq.empty,
    )
}

object SampleDataAlreadyLoaded {
  def apply(merchantId: UUID): DataError =
    ArbitraryDataError(
      message = s"Sample data can only loaded once.",
      code = "SampleDataAlreadyLoaded",
      values = Seq.empty,
    )
}

object LoyaltyProgramUniquePerMerchant {
  def apply(loyaltyProgramIds: Seq[UUID]): DataError =
    ArbitraryDataError(
      message =
        s"A merchant can have only up to one loyalty program. Existing loyalty program id: ${loyaltyProgramIds.mkString(", ")}",
      code = "LoyaltyProgramUniquePerMerchant",
      values = Seq.empty,
    )
}

object RewardRedemptionAlreadyAssociated {
  def apply(
      rewardRedemptionId: UUID,
      existingOrderId: Option[UUID],
      requestedOrderID: UUID,
    ): DataError =
    ArbitraryDataError(
      message =
        s"Reward redemption $rewardRedemptionId is already associated with $existingOrderId, but now attempting to associate with $requestedOrderID",
      code = "RewardRedemptionAlreadyAssociated",
      values = Seq.empty,
    )
}

object NotEnoughLoyaltyPoints {
  def apply(currentBalance: Int, rewardPoints: Int): DataError =
    ArbitraryDataError(
      message = s"Points to redeem the reward are $rewardPoints, but current balance is $currentBalance.",
      code = "NotEnoughLoyaltyPoints",
      values = Seq.empty,
    )
}

object EmailRequiredForLoyaltySignUp {
  def apply(): DataError =
    ArbitraryDataError(
      message = s"Email is required to sign up for a loyalty program.",
      code = "EmailRequiredForLoyaltySignUp",
      values = Seq.empty,
    )
}

case object LoyaltyMembershipCustomerNotEnrolled {
  def apply(id: UUID): DataError =
    ArbitraryDataError(
      message = s"Loyalty membership $id not enrolled.",
      code = toString,
      values = Seq(id),
    )
}

case object LoyaltyMembershipEmailNotSent {
  def apply(id: UUID): DataError =
    ArbitraryDataError(
      message = s"Can't send loyalty sign up email for membership $id.",
      code = toString,
      values = Seq.empty,
    )
}

object RewardRedemptionUnmatchedObjectId {
  def apply(upsertion: RewardRedemptionSync, ids: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = s"Could not find a ${upsertion.objectType} with id ${upsertion.objectId}",
      code = "RewardRedemptionUnmatchedObjectId",
      values = ids,
    )
}

object ZeroDiscount {
  def apply(upsertion: ItemDiscountUpsertion): DataError =
    ArbitraryDataError(
      message = s"Discount with amount=0 in $upsertion",
      code = "ZeroDiscount",
      values = Seq.empty,
    )
}

object UnexpectedNonStorableIds {
  def apply(ids: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = s"Expected only storable ids, but non-storable ids found",
      code = "UnexpectedNonStorableIds",
      values = ids,
    )
}

object InvalidAcceptanceStatus {
  def apply(status: AcceptanceStatus): DataError =
    ArbitraryDataError(
      message = s"Acceptance status $status cannot be set for an order in this state",
      code = "InvalidAcceptanceStatus",
      values = Seq.empty,
    )
}

object InvalidAcceptanceStatusChange {
  def apply(
      orderId: UUID,
      onlineOrderAttribute: OnlineOrderAttributeRecord,
      to: AcceptanceStatus,
    ): DataError =
    ArbitraryDataError(
      message =
        s"Can't transition order id $orderId / attribute ${onlineOrderAttribute.id} from ${onlineOrderAttribute.acceptanceStatus} to $to",
      code = "InvalidAcceptanceStatusChange",
      values = Seq.empty,
    )
}

object NonOnlineOrder {
  def apply(orderId: UUID): DataError =
    ArbitraryDataError(
      message = s"Order $orderId has no online attribute",
      code = "NonOnlineOrder",
      values = Seq.empty,
    )
}

object DeletionCatalogsInUse {
  def apply(catalogIds: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = s"Catalogs cannot be deleted because they are associated to a store.",
      code = "DeletionCatalogsInUse",
      values = Seq.empty,
    )
}

object DefaultMenuDeletionIsNotAllowed {
  def apply(catalogId: UUID): DataError =
    ArbitraryDataError(
      message = s"Default menu Catalog cannot be deleted.",
      code = "DefaultMenuDeletionIsNotAllowed",
      values = Seq(catalogId),
    )
}

object InvalidGiftCardPassOrderItemAssociation {
  def apply(orderItemId: UUID): DataError =
    ArbitraryDataError(
      message = s"Order Item $orderItemId is not associated to a gift card pass purchase",
      code = "InvalidGiftCardPassOrderItemAssociation",
      values = Seq(orderItemId),
    )
}

object InvalidOrderItemOrderAssociation {
  def apply(orderItemId: UUID, orderId: UUID): DataError =
    ArbitraryDataError(
      message = s"Order Items $orderItemId already belongs to another order that is not $orderId",
      code = "InvalidOrderItemOrderAssociation",
      values = Seq(orderItemId),
    )
}

object UserNotEnabledInLocation {
  def apply(userId: UUID, locationId: UUID): DataError =
    ArbitraryDataError(
      message = s"User $userId has no access to $locationId",
      code = "UserNotEnabledInLocation",
      values = Seq(userId),
    )
}

object NoUserMatchingPin {
  def apply(): DataError =
    ArbitraryDataError(
      message = s"No user matching the given pin found",
      code = "NoUserMatchingPin",
      values = Seq.empty,
    )
}

object InvalidFutureTime {
  def apply(time: ZonedDateTime): DataError =
    ArbitraryDataError(
      message = "A time in the future is not valid",
      code = "InvalidFutureTime",
      values = Seq(time),
    )
}

object InvalidLocationIdChange {
  def apply(id: UUID): DataError =
    ArbitraryDataError(
      message = "Can't change location id for existing record",
      code = "InvalidLocationIdChange",
      values = Seq(id),
    )
}

object KitchenStillInUse {
  def apply(ids: Seq[UUID]): DataError =
    ArbitraryDataError(
      message = "These kitchen ids are still in use as targets of product routing. Change them before trying again.",
      code = "KitchenStillInUse",
      values = ids,
    )
}

object MatchingKitchenIdNotFound {
  def apply(update: entities.TicketUpdate): DataError =
    ArbitraryDataError(
      message = s"Couldn't find matching kitchen given this update $update",
      code = "MatchingKitchenIdNotFound",
      values = Seq.empty,
    )
}

object KitchenLocationIdMismatch {
  def apply(kitchenLocationId: UUID, locationId: UUID): DataError =
    ArbitraryDataError(
      message = s"Kitchen location id $kitchenLocationId doesn't match ticket location id $locationId",
      code = "KitchenLocationIdMismatch",
      values = Seq.empty,
    )
}

object UnsupportedHostedDomain {
  def apply(): DataError =
    ArbitraryDataError(
      message = "The account domain you used is not whitelisted to login to this app",
      code = "UnsupportedHostedDomain",
      values = Seq.empty,
    )
}

object GoogleIdTokenValidationError {
  def apply(): DataError =
    ArbitraryDataError(
      message = "It wasn't possible to validate the provided id_token",
      code = "GoogleIdTokenValidationError",
      values = Seq.empty,
    )
}

object AdminPasswordAuthDisabledError {
  def apply(): DataError =
    ArbitraryDataError(
      message = "Admin password authentication is disabled",
      code = "AdminPasswordAuthDisabledError",
      values = Seq.empty,
    )
}

object OauthInvalidRedirectUri {
  def apply(): DataError =
    ArbitraryDataError(
      message = "Given redirect URL is not among the list of accepted redirect urls for this app",
      code = "OauthInvalidRedirectUri",
      values = Seq.empty,
    )
}

object OauthInvalidClientSecret {
  def apply(): DataError =
    ArbitraryDataError(
      message = "Given client id/secret doesn't exist or doesn't match",
      code = "OauthInvalidClientSecret",
      values = Seq.empty,
    )
}

object OauthInvalidCode {
  def apply(): DataError =
    ArbitraryDataError(
      message = "Given code doesn't exist or doesn't match",
      code = "OauthInvalidCode",
      values = Seq.empty,
    )
}

object MissingOnlineOrderAttributes {
  def apply(): DataError =
    ArbitraryDataError(
      message = "Missing online order attributes",
      code = "MissingOnlineOrderAttributes",
      values = Seq.empty,
    )
}

object ProductOutOfStock {
  def apply(productId: UUID): DataError =
    ArbitraryDataError(message = "Product is out of stock", code = "ProductOutOfStock", values = Seq(productId))
}

case object OrderWithoutAmount {
  def apply(orderId: UUID): DataError =
    ArbitraryDataError(message = s"Order without amount", code = "OrderWithoutAmount", values = Seq(orderId))
}

object PaymentTransactionInvalidAmount {
  def apply(paymentTransactionId: UUID): DataError =
    ArbitraryDataError(
      message = s"Payment transaction amount is invalid",
      code = "PaymentTransactionInvalidAmount",
      values = Seq(paymentTransactionId),
    )
}

object PaymentTransactionPartialPayment {
  def apply(paymentTransactionId: UUID): DataError =
    ArbitraryDataError(
      message = s"Payment transaction amount is for a partial payment. This is not supported right now.",
      code = "PaymentTransactionPartialPayment",
      values = Seq(paymentTransactionId),
    )
}

object InvalidRefundAmount {
  def apply(amount: BigDecimal, maxAmount: BigDecimal): DataError =
    ArbitraryDataError(
      message = s"Amount $amount is invalid. The maximum that can be refunded is $maxAmount.",
      code = "InvalidRefundAmount",
      values = Seq.empty,
    )
}

object PaymentTransactionProcessorMismatch {
  def apply(expected: TransactionPaymentProcessor, actual: TransactionPaymentProcessor): DataError =
    ArbitraryDataError(
      message = s"Expected transaction to have processor $expected, found $actual.",
      code = "PaymentTransactionProcessorMismatch",
      values = Seq.empty,
    )
}

object PaymentTransactionTypeMismatch {
  def apply(expected: TransactionType, actual: Option[TransactionType]): DataError =
    ArbitraryDataError(
      message = s"Expected transaction to have type $expected, found $actual.",
      code = "PaymentTransactionTypeMismatch",
      values = Seq.empty,
    )
}

object StripeClientError {
  def apply(response: StripeError): DataError =
    ArbitraryDataError(
      message = s"Received error from stripe client response = $response",
      code = "StripeClientError",
      values = Seq.empty,
    )
}

private object expectedString extends Function1[Seq[enumeratum.EnumEntry], String] {
  override def apply(expected: Seq[EnumEntry]): String =
    expected.map(_.entryName).mkString(" or ")
}

object MissingPaymentProcessor {
  def apply(expected: PaymentProcessor*): DataError =
    ArbitraryDataError(
      message = s"Expected merchant to have payment processor of type ${expectedString(expected)}.",
      code = "MissingPaymentProcessor",
      values = Seq.empty,
    )
}

object MissingPaymentProcessorConfig {
  def apply(expected: PaymentProcessor*): DataError =
    ArbitraryDataError(
      message = s"Expected merchant to have payment processor config for ${expectedString(expected)}.",
      code = "MissingPaymentProcessorConfig",
      values = Seq.empty,
    )
}

object UnexpectedPaymentProcessor {
  def apply(actual: PaymentProcessor, expected: PaymentProcessor*): DataError =
    ArbitraryDataError(
      message =
        s"Expected merchant to have payment processor of type ${expectedString(expected)} but got ${actual.entryName}.",
      code = "UnexpectedPaymentProcessor",
      values = Seq.empty,
    )
}

object UnexpectedPaymentProcessorConfig {
  def apply(actual: PaymentProcessor, expected: PaymentProcessor*): DataError =
    ArbitraryDataError(
      message =
        s"Expected merchant to have payment processor config for ${expectedString(expected)} but got ${actual.entryName}.",
      code = "UnexpectedPaymentProcessorConfig",
      values = Seq.empty,
    )
}

object UnexpectedMissingDefaultProductCatalog {
  def apply(merchantId: UUID): DataError =
    ArbitraryDataError(
      message = s"Expected to find Default Product Catalog for merchant $merchantId",
      code = "UnexpectedMissingDefaultProductCatalog",
      values = Seq.empty,
    )
}

case object ModifierMinimumOptionCountNotSpecified extends DataError {
  override val message: String =
    "Expected minimum_option_count to be specified, but it was not."
}

case object NeitherModifierCountsNorLegacyBooleanFlagsAreSepcified extends DataError {
  override val message: String =
    "Neither modifier counts not legacy boolean flags are specified."
}

final case class ModifierOptionErrors(errors: Nel[String]) extends DataError {
  override val message = errors.toList.mkString(" ")
  override val code = "ModifierOptionErrors"
  override val values = errors.toList
}

final case class MaximumOptionCountMustNotBeSmallerThanMaximumSingleOptionCount(
    maximumOptionCount: Int,
    maximumSingleOptionCount: Int,
  ) extends DataError {
  override val message =
    s"Expected maximum_option_count $maximumOptionCount to be >= maximum_single_option_count $maximumSingleOptionCount."

  override val code = "MaximumOptionCountMustNotBeSmallerThanMaximumSingleOptionCount"
}
