package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.core.entities.enums.CashDrawerManagementMode
import io.paytouch.core.entities.enums.TipsHandlingMode
import org.scalacheck.Gen

import scala.concurrent._

object LocationSettingsSeeds extends Seeds {

  lazy val locationSettingsDao = daos.locationSettingsDao

  def load(locations: Seq[LocationRecord])(implicit user: UserRecord): Future[Seq[LocationSettingsRecord]] = {

    val locationSettings = locations.map { location =>
      LocationSettingsUpdate(
        id = None,
        merchantId = Some(user.merchantId),
        locationId = Some(location.id),
        orderRoutingAuto = Some(genBoolean.instance),
        orderTypeDineIn = Some(genBoolean.instance),
        orderTypeTakeOut = Some(genBoolean.instance),
        orderTypeDeliveryRestaurant = Some(genBoolean.instance),
        orderTypeInStore = Some(genBoolean.instance),
        orderTypeInStorePickUp = Some(genBoolean.instance),
        orderTypeDeliveryRetail = Some(genBoolean.instance),
        webStorefrontActive = Some(genBoolean.instance),
        mobileStorefrontActive = Some(genBoolean.instance),
        facebookStorefrontActive = Some(genBoolean.instance),
        invoicesActive = Some(genBoolean.instance),
        discountBelowCostActive = Some(genBoolean.instance),
        cashDrawerManagementActive = Some(genBoolean.instance),
        cashDrawerManagement = Some(CashDrawerManagementMode.Unlocked),
        giftCardsActive = Some(genBoolean.instance),
        paymentTypeCreditCard = Some(genBoolean.instance),
        paymentTypeCash = Some(genBoolean.instance),
        paymentTypeDebitCard = Some(genBoolean.instance),
        paymentTypeCheck = Some(genBoolean.instance),
        paymentTypeGiftCard = Some(genBoolean.instance),
        paymentTypeStoreCredit = Some(genBoolean.instance),
        paymentTypeEbt = Some(genBoolean.instance),
        paymentTypeApplePay = Some(genBoolean.instance),
        tipsHandling = Some(TipsHandlingMode.TipJar),
        tipsOnDeviceEnabled = Some(genBoolean.instance),
        bypassSignatureAmount = Gen.option(genBigDecimal).instance,
        onlineStorefrontEnabled = Some(genBoolean.instance),
        deliveryProvidersEnabled = Some(genBoolean.instance),
        maxDrivingDistanceInMeters = Gen.option(genBigDecimal).instance,
        orderAutocomplete = Some(genBoolean.instance),
        preauthEnabled = Some(genBoolean.instance),
        nextOrderNumberScopeType = None,
        cfd = None,
        onlineOrder = None,
        rapidoEnabled = Some(genBoolean.instance),
      )
    }

    locationSettingsDao.bulkUpsert(locationSettings).extractRecords
  }
}
