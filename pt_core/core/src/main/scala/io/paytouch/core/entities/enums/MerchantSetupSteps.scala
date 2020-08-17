package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.data.model.enums.BusinessType
import io.paytouch.core.utils.EnumEntrySnake

sealed trait MerchantSetupSteps extends EnumEntrySnake

case object MerchantSetupSteps extends Enum[MerchantSetupSteps] {

  case object SetupLocations extends MerchantSetupSteps
  case object ImportProducts extends MerchantSetupSteps
  case object ImportCustomers extends MerchantSetupSteps
  case object SetupEmployees extends MerchantSetupSteps
  case object ScheduleEmployees extends MerchantSetupSteps
  case object DesignReceipts extends MerchantSetupSteps
  case object SetupKitchens extends MerchantSetupSteps
  case object SetupMenus extends MerchantSetupSteps
  case object SetupTaxes extends MerchantSetupSteps
  case object SetupPayments extends MerchantSetupSteps
  case object SetupOnlineStore extends MerchantSetupSteps
  case object ConnectDeliveryProvider extends MerchantSetupSteps
  case object DownloadApp extends MerchantSetupSteps

  val values = findValues

  val forBusinessType: Map[BusinessType, Seq[MerchantSetupSteps]] = Map(
    BusinessType.QSR -> Seq(
      SetupLocations,
      ImportProducts,
      ImportCustomers,
      SetupEmployees,
      ScheduleEmployees,
      DesignReceipts,
      SetupKitchens,
      SetupTaxes,
      SetupMenus,
      SetupPayments,
      SetupOnlineStore,
      ConnectDeliveryProvider,
      DownloadApp,
    ),
    BusinessType.Restaurant -> Seq(
      SetupLocations,
      ImportProducts,
      ImportCustomers,
      SetupEmployees,
      ScheduleEmployees,
      DesignReceipts,
      SetupKitchens,
      SetupTaxes,
      SetupMenus,
      SetupPayments,
      SetupOnlineStore,
      ConnectDeliveryProvider,
      DownloadApp,
    ),
    BusinessType.Retail -> Seq(
      SetupLocations,
      ImportProducts,
      ImportCustomers,
      SetupEmployees,
      ScheduleEmployees,
      DesignReceipts,
    ),
  )
}
