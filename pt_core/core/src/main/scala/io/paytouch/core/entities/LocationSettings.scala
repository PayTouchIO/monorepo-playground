package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.ScopeType
import io.paytouch.core.entities.enums._

final case class LocationSettings(
    locationId: UUID,
    orderRoutingAuto: Boolean,
    orderRoutingBar: Boolean,
    orderRoutingKitchen: Boolean,
    orderTypeDineIn: Boolean,
    orderTypeTakeOut: Boolean,
    orderTypeDeliveryRestaurant: Boolean,
    orderTypeInStore: Boolean,
    orderTypeInStorePickUp: Boolean,
    orderTypeDeliveryRetail: Boolean,
    barViewActive: Boolean,
    kitchenViewActive: Boolean,
    invoicesActive: Boolean,
    discountBelowCostActive: Boolean,
    cashDrawerManagementActive: Boolean,
    cashDrawerManagement: CashDrawerManagementMode,
    giftCardsActive: Boolean,
    paymentTypeCreditCard: Boolean,
    paymentTypeCash: Boolean,
    paymentTypeDebitCard: Boolean,
    paymentTypeCheck: Boolean,
    paymentTypeGiftCard: Boolean,
    paymentTypeStoreCredit: Boolean,
    paymentTypeEbt: Boolean,
    paymentTypeApplePay: Boolean,
    tipsEnabled: Boolean,
    tipsHandling: TipsHandlingMode,
    tipsOnDeviceEnabled: Boolean,
    bypassSignatureAmount: BigDecimal,
    onlineStorefrontEnabled: Boolean,
    deliveryProvidersEnabled: Boolean,
    orderAutocomplete: Boolean,
    preauthEnabled: Boolean,
    nextOrderNumberScopeType: ScopeType,
    locationEmailReceipt: Option[LocationEmailReceipt],
    locationPrintReceipt: Option[LocationPrintReceipt],
    locationReceipt: Option[LocationReceipt],
    cfd: CfdSettings,
    onlineOrder: OnlineOrderSettings,
    rapidoEnabled: Boolean,
  ) extends ExposedEntity {
  val classShortName = ExposedName.LocationSettings
}

final case class LocationSettingsUpdate(
    orderRoutingAuto: Option[Boolean],
    orderTypeDineIn: Option[Boolean],
    orderTypeTakeOut: Option[Boolean],
    orderTypeDeliveryRestaurant: Option[Boolean],
    orderTypeInStore: Option[Boolean],
    orderTypeInStorePickUp: Option[Boolean],
    orderTypeDeliveryRetail: Option[Boolean],
    invoicesActive: Option[Boolean],
    discountBelowCostActive: Option[Boolean],
    cashDrawerManagementActive: Option[Boolean],
    cashDrawerManagement: Option[CashDrawerManagementMode],
    giftCardsActive: Option[Boolean],
    paymentTypeCreditCard: Option[Boolean],
    paymentTypeCash: Option[Boolean],
    paymentTypeDebitCard: Option[Boolean],
    paymentTypeCheck: Option[Boolean],
    paymentTypeGiftCard: Option[Boolean],
    paymentTypeStoreCredit: Option[Boolean],
    paymentTypeEbt: Option[Boolean],
    paymentTypeApplePay: Option[Boolean],
    tipsEnabled: Option[Boolean],
    tipsHandling: Option[TipsHandlingMode],
    tipsOnDeviceEnabled: Option[Boolean],
    bypassSignatureAmount: Option[BigDecimal],
    deliveryProvidersEnabled: Option[Boolean],
    orderAutocomplete: Option[Boolean],
    preauthEnabled: Option[Boolean],
    nextOrderNumberScopeType: Option[ScopeType],
    locationEmailReceipt: Option[LocationEmailReceiptUpdate],
    locationPrintReceipt: Option[LocationPrintReceiptUpdate],
    locationReceipt: Option[LocationReceiptUpdate],
    cfd: Option[CfdSettingsUpdate],
    onlineOrder: Option[OnlineOrderSettingsUpdate],
  ) extends UpdateEntity[LocationSettings]
