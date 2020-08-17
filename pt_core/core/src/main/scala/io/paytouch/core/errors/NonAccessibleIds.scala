package io.paytouch.core.errors

import java.util.UUID

import io.paytouch.core.data.model.enums.ArticleScope

final case class NonAccessibleIds(
    message: String,
    override val code: String,
    values: Seq[UUID],
    objectId: Option[UUID] = None,
    field: Option[String] = None,
  ) extends NotFound

trait NonAccessibleIdsCreation extends ErrorMessageCodeForIds[NonAccessibleIds] {
  def apply(ids: Seq[UUID]): NonAccessibleIds =
    NonAccessibleIds(message = message, code = code, values = ids)
}

object NonAccessibleAdminIds extends NonAccessibleIdsCreation {
  val message = "Admin ids not accessible"
  val code = "NonAccessibleAdminIds"
}

object NonAccessibleMerchantIds extends NonAccessibleIdsCreation {
  val message = "Merchant ids not accessible"
  val code = "NonAccessibleMerchantIds"
}

object NonAccessibleBrandIds extends NonAccessibleIdsCreation {
  val message = "Brand ids not accessible"
  val code = "NonAccessibleBrandIds"
}

object NonAccessibleBundleOptionIds extends NonAccessibleIdsCreation {
  val message = "Bundle option ids not accessible"
  val code = "NonAccessibleBundleOptionIds"
}

object NonAccessibleBundleSetIds extends NonAccessibleIdsCreation {
  val message = "Bundle option ids not accessible"
  val code = "NonAccessibleBundleSetIds"
}

object NonAccessibleCashDrawerActivityIds extends NonAccessibleIdsCreation {
  val message = "Cash drawer activity ids not accessible"
  val code = "NonAccessibleCashDrawerActivityIds"
}

object NonAccessibleCashDrawerIds extends NonAccessibleIdsCreation {
  val message = "Cash drawer ids not accessible"
  val code = "NonAccessibleCashDrawerIds"
}

object NonAccessibleCatalogIds extends NonAccessibleIdsCreation {
  val message = "Catalog ids not accessible"
  val code = "NonAccessibleCatalogIds"
}

object NonAccessibleCategoryIds extends NonAccessibleIdsCreation {
  val message = "Category ids not accessible"
  val code = "NonAccessibleCategoryIds"
}

object NonAccessibleCommentIds extends NonAccessibleIdsCreation {
  val message = "Comment ids not accessible"
  val code = "NonAccessibleCommentIds"
}

object NonAccessibleCustomerIds extends NonAccessibleIdsCreation {
  val message = "Customer ids not accessible"
  val code = "NonAccessibleCustomerIds"
}

object NonAccessibleDiscountIds extends NonAccessibleIdsCreation {
  val message = "Discount ids not accessible"
  val code = "NonAccessibleDiscountIds"
}

object NonAccessibleGiftCardIds extends NonAccessibleIdsCreation {
  val message = "Gift card ids not accessible"
  val code = "NonAccessibleGiftCardIds"
}

object NonAccessibleGiftCardPassIds extends NonAccessibleIdsCreation {
  val message = "Gift card pass ids not accessible"
  val code = "NonAccessibleGiftCardPassIds"
}

object NonAccessibleGiftCardPassTransactionIds extends NonAccessibleIdsCreation {
  val message = "Gift card pass transaction ids not accessible"
  val code = "NonAccessibleGiftCardPassTransactionIds"
}

object NonAccessibleGroupIds extends NonAccessibleIdsCreation {
  val message = "Group ids not accessible"
  val code = "NonAccessibleGroupIds"
}

object NonAccessibleImageUploadIds extends NonAccessibleIdsCreation {
  val message = "Image upload ids not accessible"
  val code = "NonAccessibleImageUploadIds"
}

object NonAccessibleImportIds extends NonAccessibleIdsCreation {
  val message = "Import ids not accessible"
  val code = "NonAccessibleImportIds"
}

object NonAccessibleInventoryCountIds extends NonAccessibleIdsCreation {
  val message = "Inventory count ids not accessible"
  val code = "NonAccessibleInventoryCountIds"
}

object NonAccessibleKitchenIds extends NonAccessibleIdsCreation {
  val message = "Kitchen ids not accessible"
  val code = "NonAccessibleKitchenIds"
}

object NonAccessibleTicketIds extends NonAccessibleIdsCreation {
  val message = "ticket ids not accessible"
  val code = "NonAccessibleTicketIds"
}

object NonAccessibleLocationIds extends NonAccessibleIdsCreation {
  val message = "Location ids not accessible"
  val code = "NonAccessibleLocationIds"
}

object NonAccessibleLoyaltyMembershipIds extends NonAccessibleIdsCreation {
  val message = "Loyalty membership ids not accessible"
  val code = "NonAccessibleLoyaltyMembershipIds"
}

object NonAccessibleLoyaltyProgramIds extends NonAccessibleIdsCreation {
  val message = "Loyalty program ids not accessible"
  val code = "NonAccessibleLoyaltyProgramIds"
}

object NonAccessibleLoyaltyRewardIds extends NonAccessibleIdsCreation {
  val message = "Loyalty reward ids not accessible"
  val code = "NonAccessibleLoyaltyRewardIds"
}

object NonAccessibleModifierOptionIds extends NonAccessibleIdsCreation {
  val message = "Modifier option ids not accessible"
  val code = "NonAccessibleModifierOptionIds"
}

object NonAccessibleModifierSetIds extends NonAccessibleIdsCreation {
  val message = "Modifier option ids not accessible"
  val code = "NonAccessibleModifierSetIds"
}

object NonAccessibleMainProductIds extends NonAccessibleIdsCreation {
  val message = "Product ids not accessible. All product ids must belong to either a template or a simple product"
  val code = "NonAccessibleMainProductIds"
}

object NonAccessibleStorableProductIds extends NonAccessibleIdsCreation {
  val message = "Product ids not accessible. All product ids must belong to either a variant or a simple product"
  val code = "NonAccessibleStorableProductIds"
}

object NonAccessibleOauthAppIds extends NonAccessibleIdsCreation {
  val message = "OauthApp ids not accessible"
  val code = "NonAccessibleOauthAppIds"
}

object NonAccessibleOrderIds extends NonAccessibleIdsCreation {
  val message = "Order ids not accessible"
  val code = "NonAccessibleOrderIds"
}

object NonAccessibleOrderDeliveryAddressIds extends NonAccessibleIdsCreation {
  val message = "Order delivery address ids not accessible"
  val code = "NonAccessibleOrderDeliveryAddressIds"
}

object NonAccessibleOnlineOrderAttributeIds extends NonAccessibleIdsCreation {
  val message = "Online attribute order ids not accessible"
  val code = "NonAccessibleOnlineOrderAttributeIds"
}

object NonAccessibleOrderBundleIds extends NonAccessibleIdsCreation {
  val message = "Order bundle ids not accessible"
  val code = "NonAccessibleOrderBundleIds"
}

object NonAccessibleOrderDiscountIds extends NonAccessibleIdsCreation {
  val message = "Order discount ids not accessible"
  val code = "NonAccessibleOrderDiscountIds"
}

object NonAccessibleOrderItemIds extends NonAccessibleIdsCreation {
  val message = "Order item ids not accessible"
  val code = "NonAccessibleOrderItemIds"
}

object NonAccessibleOrderItemDiscountIds extends NonAccessibleIdsCreation {
  val message = "Order item discount ids not accessible"
  val code = "NonAccessibleOrderItemDiscountIds"
}

object NonAccessiblePaymentTransactionIds extends NonAccessibleIdsCreation {
  val message = "Payment transaction ids not accessible"
  val code = "NonAccessiblePaymentTransactionIds"
}

object NonAccessibleRefundedPaymentTransactionIds extends NonAccessibleIdsCreation {
  val message = "Refunded payment transaction ids not accessible"
  val code = "NonAccessibleRefundedPaymentTransactionIds"
}

object NonAccessiblePurchaseOrderIds extends NonAccessibleIdsCreation {
  val message = "Purchase order ids not accessible"
  val code = "NonAccessiblePurchaseOrderIds"
}

object NonAccessibleReceivingOrderIds extends NonAccessibleIdsCreation {
  val message = "Receiving order ids not accessible"
  val code = "NonAccessibleReceivingOrderIds"
}

object NonAccessibleReturnOrderIds extends NonAccessibleIdsCreation {
  val message = "Return order ids not accessible"
  val code = "NonAccessibleReturnOrderIds"
}

object NonAccessibleRewardRedemptionIds extends NonAccessibleIdsCreation {
  val message = "Reward redemption ids not accessible"
  val code = "NonAccessibleRewardRedemptionIds"
}

object NonAccessibleShiftIds extends NonAccessibleIdsCreation {
  val message = "Shift ids not accessible"
  val code = "NonAccessibleShiftIds"
}

object NonAccessibleStockIds extends NonAccessibleIdsCreation {
  val message = "Stock ids not accessible"
  val code = "NonAccessibleStockIds"
}

object NonAccessibleSubcategoryIds extends NonAccessibleIdsCreation {
  val message = "Subcategory ids not accessible"
  val code = "NonAccessibleSubcategoryIds"
}

object NonAccessibleSupplierIds extends NonAccessibleIdsCreation {
  val message = "Supplier ids not accessible"
  val code = "NonAccessibleSupplierIds"
}

object NonAccessibleTaxRateIds extends NonAccessibleIdsCreation {
  val message = "Tax rate ids not accessible"
  val code = "NonAccessibleTaxRateIds"
}

object NonAccessibleTimeCardIds extends NonAccessibleIdsCreation {
  val message = "Time card ids not accessible"
  val code = "NonAccessibleTimeCardIds"
}

object NonAccessibleTimeOffCardIds extends NonAccessibleIdsCreation {
  val message = "Time off card ids not accessible"
  val code = "NonAccessibleTimeOffCardIds"
}

object NonAccessibleTransferOrderIds extends NonAccessibleIdsCreation {
  val message = "Transfer order ids not accessible"
  val code = "NonAccessibleTransferOrderIds"
}

object NonAccessibleVariantOptionIds extends NonAccessibleIdsCreation {
  val message = "Variant option ids not accessible"
  val code = "NonAccessibleVariantOptionIds"
}

object NonAccessibleUserIds extends NonAccessibleIdsCreation {
  val message = "User ids not accessible"
  val code = "NonAccessibleUserIds"
}

object NonAccessibleUserRoleIds extends NonAccessibleIdsCreation {
  val message = "User role ids not accessible"
  val code = "NonAccessibleUserRoleIds"
}

object NonAccessibleUserLocationIds {

  def apply(userId: UUID, locationId: UUID): NonAccessibleIds =
    NonAccessibleIds(
      message = s"User $userId is not associated to location $locationId",
      code = "NonAccessibleUserLocationIds",
      values = Seq.empty,
    )
}

object NonAccessibleProductIds {
  def apply(ids: Seq[UUID], scope: Option[ArticleScope]): NonAccessibleIds = {
    val scopeMsg = scope.fold("")(scp => s"with scope $scp")
    NonAccessibleIds(message = s"Product ids not accessible $scopeMsg", code = "NonAccessibleProductIds", values = ids)
  }
}

object NonAccessibleTipsAssignmentIds extends NonAccessibleIdsCreation {
  val message = "TipsAssignment ids not accessible"
  val code = "NonAccessibleTipsAssignmentIds"
}

object InvalidPasswordResetToken {
  def apply(): NonAccessibleIds =
    NonAccessibleIds(
      message = "Invalid password reset token",
      code = "InvalidPasswordResetToken",
      values = Seq.empty,
    )
}
