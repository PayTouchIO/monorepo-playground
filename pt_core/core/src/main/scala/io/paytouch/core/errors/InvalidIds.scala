package io.paytouch.core.errors

import java.util.UUID

import io.paytouch.core.data.model.enums.ArticleScope

final case class InvalidIds(
    message: String,
    override val code: String,
    values: Seq[UUID],
    objectId: Option[UUID] = None,
    field: Option[String] = None,
  ) extends NotFound

trait InvalidIdsCreation extends ErrorMessageCodeForIds[InvalidIds] {
  def apply(ids: Seq[UUID]): InvalidIds = InvalidIds(message = message, code = code, values = ids)
}

object InvalidAdminIds extends InvalidIdsCreation {
  val message = "Admin ids not valid"
  val code = "InvalidAdminIds"
}

object InvalidMerchantIds extends InvalidIdsCreation {
  val message = "Merchant ids not valid"
  val code = "InvalidMerchantIds"
}

object InvalidBrandIds extends InvalidIdsCreation {
  val message = "Brand ids not valid"
  val code = "InvalidBrandIds"
}

object InvalidBundleOptionIds extends InvalidIdsCreation {
  val message = "Bundle option ids not valid"
  val code = "InvalidBundleOptionIds"
}

object InvalidBundleSetIds extends InvalidIdsCreation {
  val message = "Bundle set ids not valid"
  val code = "InvalidBundleSetIds"
}

object InvalidCashDrawerActivityIds extends InvalidIdsCreation {
  val message = "CashDrawerActivity ids not valid"
  val code = "InvalidCashDrawerActivityIds"
}

object InvalidCashDrawerIds extends InvalidIdsCreation {
  val message = "CashDrawer ids not valid"
  val code = "InvalidCashDrawerIds"
}

object InvalidCatalogIds extends InvalidIdsCreation {
  val message = "Catalog ids not valid"
  val code = "InvalidCatalogIds"
}

object InvalidCategoryIds extends InvalidIdsCreation {
  val message = "Category ids not valid"
  val code = "InvalidCategoryIds"
}

object InvalidCommentIds extends InvalidIdsCreation {
  val message = "Comment ids not valid"
  val code = "InvalidCommentIds"
}

object InvalidCustomerIds extends InvalidIdsCreation {
  val message = "Customer ids not valid"
  val code = "InvalidCustomerIds"
}

object InvalidDiscountIds extends InvalidIdsCreation {
  val message = "Discount ids not valid"
  val code = "InvalidDiscountIds"
}

object InvalidGiftCardIds extends InvalidIdsCreation {
  val message = "Gift card ids not valid"
  val code = "InvalidGiftCardIds"
}

object InvalidGiftCardPassIds extends InvalidIdsCreation {
  val message = "Gift card pass ids not valid"
  val code = "InvalidGiftCardPassIds"
}

object InvalidGiftCardPassTransactionIds extends InvalidIdsCreation {
  val message = "Gift card pass transaction ids not valid"
  val code = "InvalidGiftCardPassTransactionIds"
}

object InvalidGroupIds extends InvalidIdsCreation {
  val message = "Group ids not valid"
  val code = "InvalidGroupIds"
}

object InvalidImageUploadIds extends InvalidIdsCreation {
  val message = "ImageUpload ids not valid"
  val code = "InvalidImageUploadIds"
}

object InvalidImportIds extends InvalidIdsCreation {
  val message = "Import ids not valid"
  val code = "InvalidImportIds"
}

object InvalidInventoryCountIds extends InvalidIdsCreation {
  val message = "Inventory count ids not valid"
  val code = "InvalidInventoryCountIds"
}

object InvalidKitchenIds extends InvalidIdsCreation {
  val message = "Kitchen ids not valid"
  val code = "InvalidKitchenIds"
}

object InvalidTicketIds extends InvalidIdsCreation {
  val message = "Ticket ids not valid"
  val code = "InvalidTicketIds"
}

object InvalidLocationIds extends InvalidIdsCreation {
  val message = "Location ids not valid"
  val code = "InvalidLocationIds"
}

object InvalidLoyaltyMembershipIds extends InvalidIdsCreation {
  val message = "Loyalty membership ids not valid"
  val code = "InvalidLoyaltyMembershipIds"
}

object InvalidLoyaltyProgramIds extends InvalidIdsCreation {
  val message = "Loyalty program ids not valid"
  val code = "InvalidLoyaltyProgramIds"
}

object InvalidLoyaltyRewardIds extends InvalidIdsCreation {
  val message = "Loyalty reward ids not valid"
  val code = "InvalidLoyaltyRewardIds"
}

object InvalidMainProductIds extends InvalidIdsCreation {
  val message = "Product ids not valid. All product ids must belong to either a template or a simple product"
  val code = "InvalidMainProductIds"
}

object InvalidStorableProductIds extends InvalidIdsCreation {
  val message = "Product ids not valid. All product ids must belong to either a variant or a simple product"
  val code = "InvalidStorableProductIds"
}

object InvalidModifierOptionIds extends InvalidIdsCreation {
  val message = "Modifier option ids not valid"
  val code = "InvalidModifierOptionIds"
}

object EmptyModifierOptionIds extends InvalidIdsCreation {
  val message = "Modifier option id detected as null - register bug PR-1488"
  val code = "EmptyModifierOptionIds"
}

object InvalidModifierSetIds extends InvalidIdsCreation {
  val message = "Modifier set ids not valid"
  val code = "InvalidModifierSetIds"
}

object InvalidOauthClientIds extends InvalidIdsCreation {
  val message = "OauthClient ids not valid"
  val code = "InvalidOauthClientIds"
}

object InvalidOrderIds extends InvalidIdsCreation {
  val message = "Order ids not valid"
  val code = "InvalidOrderIds"
}

object InvalidOrderDeliveryAddressIds extends InvalidIdsCreation {
  val message = "Order delivery address ids not valid"
  val code = "InvalidOrderDeliveryAddressIds"
}

object InvalidOnlineOrderAttributeIds extends InvalidIdsCreation {
  val message = "Online order attribute ids not valid"
  val code = "InvalidOnlineOrderAttributeIds"
}

object InvalidOrderBundleIds extends InvalidIdsCreation {
  val message = "Order bundle ids not valid"
  val code = "InvalidOrderBundleIds"
}

object InvalidOrderDiscountIds extends InvalidIdsCreation {
  val message = "Order discount ids not valid"
  val code = "InvalidOrderDiscountIds"
}

object InvalidOrderItemIds extends InvalidIdsCreation {
  val message = "Order item ids not valid"
  val code = "InvalidOrderItemIds"
}

object InvalidOrderItemDiscountIds extends InvalidIdsCreation {
  val message = "Order item discount ids not valid"
  val code = "InvalidOrderItemDiscountIds"
}

object InvalidPaymentTransactionIds extends InvalidIdsCreation {
  val message = "Payment transaction ids not valid"
  val code = "InvalidPaymentTransactionIds"
}

object InvalidPurchaseOrderIds extends InvalidIdsCreation {
  val message = "Purchase order ids not valid"
  val code = "InvalidPurchaseOrderIds"
}

object InvalidReceivingOrderIds extends InvalidIdsCreation {
  val message = "Receiving order ids not valid"
  val code = "InvalidReceivingOrderIds"
}

object InvalidReturnOrderIds extends InvalidIdsCreation {
  val message = "Return order ids not valid"
  val code = "InvalidReturnOrderIds"
}

object InvalidRewardRedemptionIds extends InvalidIdsCreation {
  val message = "Reward redemption ids not valid"
  val code = "InvalidRewardRedemptionIds"
}

object InvalidShiftIds extends InvalidIdsCreation {
  val message = "Shift ids not valid"
  val code = "InvalidShiftIds"
}

object InvalidStockIds extends InvalidIdsCreation {
  val message = "Stock ids not valid"
  val code = "InvalidStockIds"
}

object InvalidSubcategoryIds extends InvalidIdsCreation {
  val message = "Subcategory ids not valid"
  val code = "InvalidSubcategoryIds"
}

object InvalidSupplierIds extends InvalidIdsCreation {
  val message = "Supplier ids not valid"
  val code = "InvalidSupplierIds"
}

object InvalidTaxRateIds extends InvalidIdsCreation {
  val message = "Tax rate ids not valid"
  val code = "InvalidTaxRateIds"
}

object EmptyTaxRateIds extends InvalidIdsCreation {
  val message = "Tax rate id detected as null - register bug PR-1488"
  val code = "EmptyTaxRateIds"
}

object InvalidTimeCardIds extends InvalidIdsCreation {
  val message = "Time card ids not valid"
  val code = "InvalidTimeCardIds"
}

object InvalidTimeOffCardIds extends InvalidIdsCreation {
  val message = "Time off card ids not valid"
  val code = "InvalidTimeOffCardIds"
}

object InvalidTransferOrderIds extends InvalidIdsCreation {
  val message = "Transfer order ids not valid"
  val code = "InvalidTransferOrderIds"
}

object InvalidVariantOptionIds extends InvalidIdsCreation {
  val message = "Variant option ids not valid"
  val code = "InvalidVariantOptionIds"
}

object EmptyVariantOptionIds extends InvalidIdsCreation {
  val message = "Variant option id detected as null - register bug PR-1488"
  val code = "EmptyVariantOptionIds"
}

object InvalidUserIds extends InvalidIdsCreation {
  val message = "User ids not valid"
  val code = "InvalidUserIds"
}

object InvalidUserRoleIds extends InvalidIdsCreation {
  val message = "User role ids not valid"
  val code = "InvalidUserRoleIds"
}

object InvalidProductIds {

  def apply(ids: Seq[UUID], scope: Option[ArticleScope]): InvalidIds = {
    val scopeMsg = scope.fold("")(scp => s"with scope $scp")
    InvalidIds(message = s"Product ids not valid $scopeMsg", code = "InvalidProductIds", values = ids)
  }
}

object InvalidTipsAssignmentIds extends InvalidIdsCreation {
  val message = "TipsAssignment ids not valid"
  val code = "InvalidTipsAssignmentIds"
}

object InvalidFeatureGroupIds extends InvalidIdsCreation {
  val message = "FeatureGroup ids not valid"
  val code = "InvalidFeatureGroupIds"
}
