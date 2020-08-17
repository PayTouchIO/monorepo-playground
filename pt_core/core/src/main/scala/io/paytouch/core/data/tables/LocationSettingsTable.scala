package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.extensions.OneToOneLocationColumns
import io.paytouch.core.data.model.{ CfdSettings, LocationSettingsRecord, OnlineOrderSettings }
import io.paytouch.core.data.model.enums.ScopeType
import io.paytouch.core.entities.enums.CashDrawerManagementMode
import io.paytouch.core.entities.enums.TipsHandlingMode
import shapeless.{ Generic, HNil }
import slickless._

class LocationSettingsTable(tag: Tag)
    extends SlickMerchantTable[LocationSettingsRecord](tag, "location_settings")
       with OneToOneLocationColumns {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def locationId = column[UUID]("location_id")

  def orderRoutingAuto = column[Boolean]("order_routing_auto")
  def orderTypeDineIn = column[Boolean]("order_type_dine_in")
  def orderTypeTakeOut = column[Boolean]("order_type_take_out")
  def orderTypeDeliveryRestaurant = column[Boolean]("order_type_delivery_restaurant")
  def orderTypeInStore = column[Boolean]("order_type_in_store")
  def orderTypeInStorePickUp = column[Boolean]("order_type_in_store_pick_up")
  def orderTypeDeliveryRetail = column[Boolean]("order_type_delivery_retail")
  def webStorefrontActive = column[Boolean]("web_storefront_active")
  def mobileStorefrontActive = column[Boolean]("mobile_storefront_active")
  def facebookStorefrontActive = column[Boolean]("facebook_storefront_active")
  def invoicesActive = column[Boolean]("invoices_active")
  def discountBelowCostActive = column[Boolean]("discount_below_cost_active")
  def cashDrawerManagementActive = column[Boolean]("cash_drawer_management_active")
  def cashDrawerManagement = column[CashDrawerManagementMode]("cash_drawer_management")
  def giftCardsActive = column[Boolean]("gift_cards_active")
  def paymentTypeCreditCard = column[Boolean]("payment_type_credit_card")
  def paymentTypeCash = column[Boolean]("payment_type_cash")
  def paymentTypeDebitCard = column[Boolean]("payment_type_debit_card")
  def paymentTypeCheck = column[Boolean]("payment_type_check")
  def paymentTypeGiftCard = column[Boolean]("payment_type_gift_card")
  def paymentTypeStoreCredit = column[Boolean]("payment_type_store_credit")
  def paymentTypeEbt = column[Boolean]("payment_type_ebt")
  def paymentTypeApplePay = column[Boolean]("payment_type_apple_pay")
  def tipsHandling = column[TipsHandlingMode]("tips_handling")
  def tipsOnDeviceEnabled = column[Boolean]("tips_on_device_enabled")
  def bypassSignatureAmount = column[BigDecimal]("bypass_signature_amount")
  def onlineStorefrontEnabled = column[Boolean]("online_storefront_enabled")
  def deliveryProvidersEnabled = column[Boolean]("delivery_providers_enabled")
  def maxDrivingDistanceInMeters = column[Option[BigDecimal]]("max_driving_distance_in_meters")
  def orderAutocomplete = column[Boolean]("order_autocomplete")
  def preauthEnabled = column[Boolean]("preauth_enabled")
  def nextOrderNumberScopeType = column[ScopeType]("next_order_number_scope_type")
  def cfd = column[Option[CfdSettings]]("cfd")
  def onlineOrder = column[Option[OnlineOrderSettings]]("online_order")
  def rapidoEnabled = column[Boolean]("rapido_enabled")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * = {
    val locationSettingsGeneric = Generic[LocationSettingsRecord]
    (id :: merchantId :: locationId ::
      orderRoutingAuto :: orderTypeDineIn :: orderTypeTakeOut ::
      orderTypeDeliveryRestaurant :: orderTypeInStore :: orderTypeInStorePickUp :: orderTypeDeliveryRetail ::
      webStorefrontActive :: mobileStorefrontActive :: facebookStorefrontActive ::
      invoicesActive :: discountBelowCostActive :: cashDrawerManagementActive :: cashDrawerManagement :: giftCardsActive ::
      paymentTypeCreditCard :: paymentTypeCash :: paymentTypeDebitCard :: paymentTypeCheck ::
      paymentTypeGiftCard :: paymentTypeStoreCredit :: paymentTypeEbt :: paymentTypeApplePay :: tipsHandling ::
      tipsOnDeviceEnabled :: bypassSignatureAmount :: onlineStorefrontEnabled :: deliveryProvidersEnabled :: maxDrivingDistanceInMeters
      :: orderAutocomplete :: preauthEnabled :: nextOrderNumberScopeType :: cfd :: onlineOrder :: rapidoEnabled ::
      createdAt :: updatedAt :: HNil)
      .shaped
      .<>(
        (dbRow: locationSettingsGeneric.Repr) => locationSettingsGeneric.from(dbRow),
        (caseClass: LocationSettingsRecord) => Some(locationSettingsGeneric.to(caseClass)),
      )
  }
}
