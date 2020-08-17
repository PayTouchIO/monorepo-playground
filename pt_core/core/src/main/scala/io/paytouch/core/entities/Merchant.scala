package io.paytouch.core.entities

import java.time.{ ZoneId, ZonedDateTime }
import java.util.{ Currency, UUID }

import cats.implicits._

import io.paytouch._
import io.paytouch.core.data._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.enums.{ ExposedName, MerchantSetupStatus, MerchantSetupSteps }

final case class Merchant(
    id: UUID,
    businessType: BusinessType,
    restaurantType: RestaurantType,
    currency: Currency,
    paymentProcessor: PaymentProcessor,
    paymentProcessorConfig: PaymentProcessorConfig,
    name: String,
    displayName: String,
    logoUrls: Seq[ImageUrls],
    ownerUser: Option[User],
    mode: MerchantMode,
    loadingStatus: LoadingStatus,
    locations: Option[Seq[Location]],
    defaultZoneId: ZoneId,
    setupCompleted: Boolean,
    setupSteps: Option[Map[MerchantSetupSteps, MerchantSetupStatus]],
    features: MerchantFeatures,
    legalDetails: Option[LegalDetails],
    legalCountry: Country,
    setupType: SetupType,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Merchant
}

trait BasePublicMerchantCreation {
  def businessType: BusinessType
  def businessName: String
  def address: AddressUpsertion
  def restaurantType: RestaurantType
  def currency: Currency
  def zoneId: ZoneId
  def pin: Option[String]
  def mode: MerchantMode
  def setupType: SetupType
  def dummyData: Boolean
}

final case class PublicMerchantCreation(
    businessType: BusinessType,
    businessName: String,
    address: AddressUpsertion = AddressUpsertion.empty,
    restaurantType: RestaurantType,
    currency: Currency,
    firstName: String,
    lastName: String,
    password: String,
    email: String,
    zoneId: ZoneId,
    pin: Option[String],
    mode: MerchantMode = MerchantMode.Demo,
    setupType: SetupType = SetupType.Paytouch,
    dummyData: Boolean = false,
  ) extends BasePublicMerchantCreation {
  def toMerchantCreation =
    MerchantCreation(
      businessType = businessType,
      businessName = businessName,
      address = address,
      restaurantType = restaurantType,
      currency = currency,
      firstName = firstName,
      lastName = lastName,
      password = password,
      email = email,
      zoneId = zoneId,
      pin = pin,
      mode = mode,
      setupType = setupType,
      dummyData = dummyData,
    )
}

final case class MerchantCreation(
    businessType: BusinessType,
    businessName: String,
    address: AddressUpsertion = AddressUpsertion.empty,
    restaurantType: RestaurantType,
    currency: Currency,
    firstName: String,
    lastName: String,
    password: String,
    email: String,
    auth0UserId: Option[Auth0UserId] = None,
    zoneId: ZoneId,
    pin: Option[String],
    mode: MerchantMode = MerchantMode.Demo,
    features: Option[MerchantFeaturesUpsertion] = None,
    legalDetails: Option[LegalDetailsUpsertion] = None,
    setupType: SetupType = SetupType.Paytouch,
    dummyData: Boolean = false,
  ) extends HasLegalDetailsUpsertion

trait HasLegalDetailsUpsertion {
  def legalDetails: Option[LegalDetailsUpsertion]
}

final case class AdminMerchantUpdate(
    businessType: Option[BusinessType],
    businessName: Option[String],
    restaurantType: Option[RestaurantType],
    worldpay: Option[model.PaymentProcessorConfig.Worldpay] = None,
    zoneId: Option[ZoneId],
    features: Option[MerchantFeaturesUpsertion],
    currency: Option[Currency],
    legalDetails: Option[LegalDetailsUpsertion],
    setupType: Option[SetupType] = None,
  ) extends HasLegalDetailsUpsertion

object AdminMerchantUpdate {
  def empty: AdminMerchantUpdate =
    AdminMerchantUpdate(
      businessType = None,
      businessName = None,
      restaurantType = None,
      worldpay = None,
      zoneId = None,
      features = None,
      currency = None,
      legalDetails = None,
      setupType = None,
    )
}

final case class ApiMerchantUpdate(
    businessName: Option[String],
    restaurantType: Option[RestaurantType] = RestaurantType.Default.some,
    zoneId: Option[ZoneId],
    legalDetails: Option[LegalDetailsUpsertion],
  ) extends UpdateEntity[Merchant]
       with HasLegalDetailsUpsertion
