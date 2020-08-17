package io.paytouch.core.conversions

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.data.model.enums.{ BusinessType, ScopeType }
import io.paytouch.core.data.model.{
  LocationSettingsUpdate => LocationSettingsUpdateModel,
  CfdSettingsUpdate => CfdSettingsUpdateModel,
  CfdSettings => CfdSettingsModel,
  OnlineOrderSettingsUpdate => OnlineOrderSettingsUpdateModel,
  OnlineOrderSettings => OnlineOrderSettingsModel,
  _,
}
import io.paytouch.core.entities.enums.CashDrawerManagementMode
import io.paytouch.core.entities.enums.TipsHandlingMode
import io.paytouch.core.entities.{
  ImageUrls,
  LocationEmailReceipt,
  LocationPrintReceipt,
  LocationReceipt,
  UserContext,
  LocationSettings => LocationSettingsEntity,
  LocationSettingsUpdate => LocationSettingsUpdateEntity,
  CfdSettings => CfdSettingsEntity,
  OnlineOrderSettings => OnlineOrderSettingsEntity,
}

trait LocationSettingsConversions
    extends EntityConversion[LocationSettingsRecord, LocationSettingsEntity]
       with ModelConversion[LocationSettingsUpdateEntity, LocationSettingsUpdateModel]
       with LazyLogging {
  def toDefaultLocationSettings(
      locationId: UUID,
      latestSettings: Option[LocationSettingsRecord],
    )(implicit
      user: UserContext,
    ) =
    latestSettings match {
      case Some(settings) => copyFromExistingSettings(locationId, settings)
      case None           => createDefaultSettings(locationId)
    }

  private def copyFromExistingSettings(locationId: UUID, settings: LocationSettingsRecord)(implicit user: UserContext) =
    LocationSettingsUpdateModel(
      id = None,
      merchantId = Some(user.merchantId),
      locationId = Some(locationId),
      orderRoutingAuto = Some(settings.orderRoutingAuto),
      orderTypeDineIn = Some(settings.orderTypeDineIn),
      orderTypeTakeOut = Some(settings.orderTypeTakeOut),
      orderTypeDeliveryRestaurant = Some(settings.orderTypeDeliveryRestaurant),
      orderTypeInStore = Some(settings.orderTypeInStore),
      orderTypeInStorePickUp = Some(settings.orderTypeInStorePickUp),
      orderTypeDeliveryRetail = Some(settings.orderTypeDeliveryRetail),
      webStorefrontActive = Some(settings.webStorefrontActive),
      mobileStorefrontActive = Some(settings.mobileStorefrontActive),
      facebookStorefrontActive = Some(settings.facebookStorefrontActive),
      invoicesActive = Some(settings.invoicesActive),
      discountBelowCostActive = Some(settings.discountBelowCostActive),
      cashDrawerManagementActive = Some(settings.cashDrawerManagementActive),
      cashDrawerManagement = Some(settings.cashDrawerManagement),
      giftCardsActive = Some(settings.giftCardsActive),
      paymentTypeCreditCard = Some(settings.paymentTypeCreditCard),
      paymentTypeCash = Some(settings.paymentTypeCash),
      paymentTypeDebitCard = Some(settings.paymentTypeDebitCard),
      paymentTypeCheck = Some(settings.paymentTypeCheck),
      paymentTypeGiftCard = Some(settings.paymentTypeGiftCard),
      paymentTypeStoreCredit = Some(settings.paymentTypeStoreCredit),
      paymentTypeEbt = Some(settings.paymentTypeEbt),
      paymentTypeApplePay = Some(settings.paymentTypeApplePay),
      tipsHandling = Some(settings.tipsHandling),
      tipsOnDeviceEnabled = Some(settings.tipsOnDeviceEnabled),
      bypassSignatureAmount = Some(settings.bypassSignatureAmount),
      onlineStorefrontEnabled = Some(settings.onlineStorefrontEnabled),
      deliveryProvidersEnabled = Some(settings.deliveryProvidersEnabled),
      maxDrivingDistanceInMeters = settings.maxDrivingDistanceInMeters,
      orderAutocomplete = Some(settings.orderAutocomplete),
      preauthEnabled = Some(settings.preauthEnabled),
      nextOrderNumberScopeType = Some(settings.nextOrderNumberScopeType),
      cfd = cfdSettingsCopy(settings.cfd),
      onlineOrder = onlineOrderSettingsCopy(settings.onlineOrder),
      rapidoEnabled = Some(settings.rapidoEnabled),
    )

  private def createDefaultSettings(locationId: UUID)(implicit user: UserContext) = {
    val isRestaurant = user.businessType == BusinessType.Restaurant || user.businessType == BusinessType.QSR
    val isRetail = user.businessType == BusinessType.Retail
    LocationSettingsUpdateModel(
      id = None,
      merchantId = Some(user.merchantId),
      locationId = Some(locationId),
      orderRoutingAuto = Some(isRestaurant),
      orderTypeDineIn = Some(isRestaurant),
      orderTypeTakeOut = Some(isRestaurant),
      orderTypeDeliveryRestaurant = Some(isRestaurant),
      orderTypeInStore = Some(true),
      orderTypeInStorePickUp = Some(true),
      orderTypeDeliveryRetail = Some(isRetail),
      webStorefrontActive = None,
      mobileStorefrontActive = None,
      facebookStorefrontActive = None,
      invoicesActive = None,
      discountBelowCostActive = None,
      cashDrawerManagementActive = Some(true),
      cashDrawerManagement = Some(CashDrawerManagementMode.Unlocked),
      giftCardsActive = Some(true),
      paymentTypeCreditCard = Some(true),
      paymentTypeCash = Some(true),
      paymentTypeDebitCard = Some(true),
      paymentTypeCheck = Some(true),
      paymentTypeGiftCard = Some(true),
      paymentTypeStoreCredit = None,
      paymentTypeEbt = None,
      paymentTypeApplePay = None,
      tipsHandling = Some(TipsHandlingMode.TipJar),
      tipsOnDeviceEnabled = Some(isRestaurant),
      bypassSignatureAmount = None,
      onlineStorefrontEnabled = Some(false),
      deliveryProvidersEnabled = Some(false),
      maxDrivingDistanceInMeters = None,
      orderAutocomplete = Some(true),
      preauthEnabled = Some(false),
      nextOrderNumberScopeType = Some(ScopeType.Location),
      cfd = None,
      onlineOrder = None,
      rapidoEnabled = Some(false),
    )
  }

  def fromRecordsAndOptionsToEntities(
      records: Seq[LocationSettingsRecord],
      emailReceiptsPerLocation: Map[UUID, LocationEmailReceipt],
      printReceiptsPerLocation: Map[UUID, LocationPrintReceipt],
      receiptsPerLocation: Map[UUID, LocationReceipt],
      splashImageUrlsPerLocation: Map[UUID, Seq[ImageUrls]],
    ): Seq[LocationSettingsEntity] =
    records.map { record =>
      val locationEmailReceipt = emailReceiptsPerLocation.get(record.locationId)
      val locationPrintReceipt = printReceiptsPerLocation.get(record.locationId)
      val locationReceipt = receiptsPerLocation.get(record.locationId)
      val splashImageUrls = splashImageUrlsPerLocation.get(record.locationId).getOrElse(Seq.empty)
      fromRecordAndOptionsToEntity(record, locationEmailReceipt, locationPrintReceipt, locationReceipt, splashImageUrls)
    }

  def fromRecordToEntity(record: LocationSettingsRecord)(implicit user: UserContext): LocationSettingsEntity =
    fromRecordAndOptionsToEntity(record, None, None, None, Seq.empty)

  def fromRecordAndOptionsToEntity(
      record: LocationSettingsRecord,
      locationEmailReceipt: Option[LocationEmailReceipt],
      locationPrintReceipt: Option[LocationPrintReceipt],
      locationReceipt: Option[LocationReceipt],
      splashImageUrls: Seq[ImageUrls],
    ): LocationSettingsEntity =
    LocationSettingsEntity(
      locationId = record.locationId,
      orderRoutingAuto = record.orderRoutingAuto,
      orderRoutingBar = true,
      orderRoutingKitchen = true,
      orderTypeDineIn = record.orderTypeDineIn,
      orderTypeTakeOut = record.orderTypeTakeOut,
      orderTypeDeliveryRestaurant = record.orderTypeDeliveryRestaurant,
      orderTypeInStore = record.orderTypeInStore,
      orderTypeInStorePickUp = record.orderTypeInStorePickUp,
      orderTypeDeliveryRetail = record.orderTypeDeliveryRetail,
      barViewActive = true,
      kitchenViewActive = true,
      invoicesActive = record.invoicesActive,
      discountBelowCostActive = record.discountBelowCostActive,
      cashDrawerManagementActive = record.cashDrawerManagementActive,
      cashDrawerManagement = record.cashDrawerManagement,
      giftCardsActive = record.giftCardsActive,
      paymentTypeCreditCard = record.paymentTypeCreditCard,
      paymentTypeCash = record.paymentTypeCash,
      paymentTypeDebitCard = record.paymentTypeDebitCard,
      paymentTypeCheck = record.paymentTypeCheck,
      paymentTypeGiftCard = record.paymentTypeGiftCard,
      paymentTypeStoreCredit = record.paymentTypeStoreCredit,
      paymentTypeEbt = record.paymentTypeEbt,
      paymentTypeApplePay = record.paymentTypeApplePay,
      tipsEnabled = record.tipsHandling == TipsHandlingMode.TipJar,
      tipsHandling = record.tipsHandling,
      tipsOnDeviceEnabled = record.tipsOnDeviceEnabled,
      bypassSignatureAmount = record.bypassSignatureAmount,
      onlineStorefrontEnabled = record.onlineStorefrontEnabled,
      deliveryProvidersEnabled = record.deliveryProvidersEnabled,
      orderAutocomplete = record.orderAutocomplete,
      preauthEnabled = record.preauthEnabled,
      nextOrderNumberScopeType = record.nextOrderNumberScopeType,
      locationEmailReceipt = locationEmailReceipt,
      locationPrintReceipt = locationPrintReceipt,
      locationReceipt = locationReceipt,
      cfd = cfdSettingsToEntity(record.cfd, splashImageUrls),
      onlineOrder = onlineOrderSettingsToEntity(record.onlineOrder),
      rapidoEnabled = record.rapidoEnabled,
    )

  def fromUpsertionToUpdate(
      locationId: UUID,
      update: LocationSettingsUpdateEntity,
    )(implicit
      user: UserContext,
    ): LocationSettingsUpdateModel = {
    val (cashDrawerManagementActive, cashDrawerManagement) = handleCashDrawerManagementConversion(update)
    val tipsHandling = handleTipsEnabledConversion(update)
    LocationSettingsUpdateModel(
      id = None,
      merchantId = Some(user.merchantId),
      locationId = Some(locationId),
      orderRoutingAuto = update.orderRoutingAuto,
      orderTypeDineIn = update.orderTypeDineIn,
      orderTypeTakeOut = update.orderTypeTakeOut,
      orderTypeDeliveryRestaurant = update.orderTypeDeliveryRestaurant,
      orderTypeInStore = update.orderTypeInStore,
      orderTypeInStorePickUp = update.orderTypeInStorePickUp,
      orderTypeDeliveryRetail = update.orderTypeDeliveryRetail,
      webStorefrontActive = None, // Deprecated
      mobileStorefrontActive = None, // Deprecated
      facebookStorefrontActive = None, // Deprecated
      invoicesActive = update.invoicesActive,
      discountBelowCostActive = update.discountBelowCostActive,
      cashDrawerManagementActive = cashDrawerManagementActive,
      cashDrawerManagement = cashDrawerManagement,
      giftCardsActive = update.giftCardsActive,
      paymentTypeCreditCard = update.paymentTypeCreditCard,
      paymentTypeCash = update.paymentTypeCash,
      paymentTypeDebitCard = update.paymentTypeDebitCard,
      paymentTypeCheck = update.paymentTypeCheck,
      paymentTypeGiftCard = update.paymentTypeGiftCard,
      paymentTypeStoreCredit = update.paymentTypeStoreCredit,
      paymentTypeEbt = update.paymentTypeEbt,
      paymentTypeApplePay = update.paymentTypeApplePay,
      tipsHandling = tipsHandling,
      tipsOnDeviceEnabled = update.tipsOnDeviceEnabled,
      bypassSignatureAmount = update.bypassSignatureAmount,
      onlineStorefrontEnabled = None, // updated via StoresActiveChanged sqs msg
      deliveryProvidersEnabled = update.deliveryProvidersEnabled,
      maxDrivingDistanceInMeters = None, // Deprecated
      orderAutocomplete = update.orderAutocomplete,
      preauthEnabled = update.preauthEnabled,
      nextOrderNumberScopeType = update.nextOrderNumberScopeType,
      cfd = update.cfd.map(_.toUpdateModel()),
      onlineOrder = update.onlineOrder.map(_.toUpdateModel()),
      rapidoEnabled = None, // updated via RapidoChanged sqs msg
    )
  }

  private def handleCashDrawerManagementConversion(
      update: LocationSettingsUpdateEntity,
    ): (Option[Boolean], Option[CashDrawerManagementMode]) =
    (update.cashDrawerManagementActive, update.cashDrawerManagement) match {
      case (Some(true), None)  => (Some(true), Some(CashDrawerManagementMode.Unlocked))
      case (Some(false), None) => (Some(false), Some(CashDrawerManagementMode.Disabled))
      case (Some(_), Some(mode)) =>
        logger.warn(
          "Both cashDrawerManagementActive and cashDrawerManagement given, ignoring cashDrawerManagementActive",
        )
        optionsForCashDrawerManagementMode(mode)
      case (None, Some(mode)) => optionsForCashDrawerManagementMode(mode)
      case (None, None)       => (None, None)
    }

  private def optionsForCashDrawerManagementMode(
      mode: CashDrawerManagementMode,
    ): (Option[Boolean], Option[CashDrawerManagementMode]) =
    mode match {
      case CashDrawerManagementMode.Unlocked => (Some(true), Some(CashDrawerManagementMode.Unlocked))
      case CashDrawerManagementMode.Locked   => (Some(true), Some(CashDrawerManagementMode.Locked))
      case CashDrawerManagementMode.Disabled => (Some(false), Some(CashDrawerManagementMode.Disabled))
    }

  private def handleTipsEnabledConversion(update: LocationSettingsUpdateEntity): Option[TipsHandlingMode] =
    (update.tipsEnabled, update.tipsHandling) match {
      case (Some(true), None)  => Some(TipsHandlingMode.TipJar)
      case (Some(false), None) => Some(TipsHandlingMode.Disabled)
      case (Some(_), Some(mode)) =>
        logger.warn("Both tipsEnabled and tipsHandling given, ignoring tipsEnabled")
        Some(mode)
      case (None, Some(mode)) => Some(mode)
      case (None, None)       => None
    }

  private def cfdSettingsCopy(model: Option[CfdSettingsModel]): Option[CfdSettingsUpdateModel] =
    model.map(_.toUpdate)

  private def cfdSettingsToEntity(model: Option[CfdSettingsModel], splashImageUrls: Seq[ImageUrls]): CfdSettingsEntity =
    model.map(_.toEntity).getOrElse(CfdSettingsEntity()).copy(splashImageUrls = splashImageUrls)

  private def onlineOrderSettingsToEntity(model: Option[OnlineOrderSettingsModel]): OnlineOrderSettingsEntity =
    model.map(_.toEntity).getOrElse(OnlineOrderSettingsEntity())

  private def onlineOrderSettingsCopy(model: Option[OnlineOrderSettingsModel]): Option[OnlineOrderSettingsUpdateModel] =
    model.map(_.toUpdate)
}
