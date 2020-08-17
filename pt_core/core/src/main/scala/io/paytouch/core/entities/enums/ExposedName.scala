package io.paytouch.core.entities.enums

import io.paytouch.core.utils.EnumEntrySnake

import enumeratum.Enum

sealed abstract class ExposedName extends EnumEntrySnake

case object ExposedName extends Enum[ExposedName] {
  case object Admin extends ExposedName
  case object Brand extends ExposedName
  case object CashDrawer extends ExposedName
  case object CashDrawerActivity extends ExposedName
  case object CashDrawerReason extends ExposedName
  case object CashDrawerReportPayload extends ExposedName
  case object Catalog extends ExposedName
  case object Category extends ExposedName
  case object Comment extends ExposedName
  case object Customer extends ExposedName
  case object Discount extends ExposedName
  case object Event extends ExposedName
  case object Export extends ExposedName
  case object ExportDownload extends ExposedName
  case object GiftCard extends ExposedName
  case object GiftCardPass extends ExposedName
  case object GiftCardPassChargeFailure extends ExposedName
  case object GiftCardPassTransaction extends ExposedName
  case object Group extends ExposedName
  case object ImageUpload extends ExposedName
  case object Import extends ExposedName
  case object InventoryCount extends ExposedName
  case object InventoryCountProduct extends ExposedName
  case object Jwt extends ExposedName
  case object Kitchen extends ExposedName
  case object LegalDetails extends ExposedName
  case object Location extends ExposedName
  case object LocationEmailReceipt extends ExposedName
  case object LocationPrintReceipt extends ExposedName
  case object LocationReceipt extends ExposedName
  case object LocationSettings extends ExposedName
  case object LocationSettingsInfo extends ExposedName
  case object LoginResponse extends ExposedName
  case object LoyaltyMembership extends ExposedName
  case object LoyaltyProgram extends ExposedName
  case object LoyaltyReward extends ExposedName
  case object Merchant extends ExposedName
  case object MerchantFeatures extends ExposedName
  case object ModifierOption extends ExposedName
  case object ModifierSet extends ExposedName
  case object OauthCode extends ExposedName
  case object Order extends ExposedName
  case object OrderFeedback extends ExposedName
  case object OrderItem extends ExposedName
  case object OrderItemDiscount extends ExposedName
  case object OrderItemModifierOption extends ExposedName
  case object OrderItemVariantOption extends ExposedName
  case object OrderRoutingStatuses extends ExposedName
  case object OrderRoutingTicket extends ExposedName
  case object OrderRoutingTicketInfo extends ExposedName
  case object PasswordResetToken extends ExposedName
  case object Payroll extends ExposedName
  case object PrepareCashDrawerReportPayload extends ExposedName
  case object Product extends ExposedName
  case object ProductCostChange extends ExposedName
  case object ProductInventory extends ExposedName
  case object ProductPart extends ExposedName
  case object ProductPriceChange extends ExposedName
  case object ProductQuantityChange extends ExposedName
  case object ProductRevenue extends ExposedName
  case object PurchaseOrder extends ExposedName
  case object PurchaseOrderProduct extends ExposedName
  case object ReceivingOrder extends ExposedName
  case object ReceivingOrderProductDetails extends ExposedName
  case object ReturnOrder extends ExposedName
  case object ReturnOrderProduct extends ExposedName
  case object RewardRedemption extends ExposedName
  case object Seating extends ExposedName
  case object Session extends ExposedName
  case object Shift extends ExposedName
  case object Stock extends ExposedName
  case object Store extends ExposedName
  case object StripeRefund extends ExposedName
  case object Supplier extends ExposedName
  case object TaxRate extends ExposedName
  case object TimeCard extends ExposedName
  case object TimeOffCard extends ExposedName
  case object TipsAssignment extends ExposedName
  case object TransferOrder extends ExposedName
  case object TransferOrderProduct extends ExposedName
  case object User extends ExposedName
  case object UserContext extends ExposedName
  case object UserRole extends ExposedName

  val values = findValues

  val toTrack =
    Seq(
      Category,
      Customer,
      Discount,
      GiftCardPassChargeFailure, // seems useful
      Kitchen,
      Location,
      LoyaltyProgram,
      ModifierSet,
      Order,
      Product,
      Supplier,
      TaxRate,
      User,
    )
}
