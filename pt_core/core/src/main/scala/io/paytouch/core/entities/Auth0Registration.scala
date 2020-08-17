package io.paytouch.core.entities

import java.util.Currency
import java.time.ZoneId

import io.paytouch.core.data._
import io.paytouch.core.data.model.enums._

final case class Auth0Registration(
    token: String,
    businessType: BusinessType,
    businessName: String,
    address: AddressUpsertion = AddressUpsertion.empty,
    restaurantType: RestaurantType,
    currency: Currency,
    zoneId: ZoneId,
    pin: Option[String],
    mode: MerchantMode = MerchantMode.Demo,
    setupType: SetupType = SetupType.Paytouch,
    dummyData: Boolean = false,
  ) extends BasePublicMerchantCreation
