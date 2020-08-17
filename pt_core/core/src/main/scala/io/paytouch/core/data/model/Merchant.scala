package io.paytouch.core.data.model

import java.time.{ ZoneId, ZonedDateTime }
import java.util.{ Currency, UUID }

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core._
import io.paytouch.core.data.model.enums.{ SetupType, _ }
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.services.UtilService

final case class MerchantRecord(
    id: UUID,
    active: Boolean,
    businessType: BusinessType,
    businessName: String,
    restaurantType: RestaurantType,
    paymentProcessor: PaymentProcessor,
    paymentProcessorConfig: PaymentProcessorConfig,
    currency: Currency,
    mode: MerchantMode,
    switchMerchantId: Option[UUID],
    setupSteps: Option[Map[MerchantSetupSteps, entities.MerchantSetupStep]],
    setupCompleted: Boolean,
    loadingStatus: LoadingStatus,
    defaultZoneId: ZoneId,
    features: entities.MerchantFeatures,
    legalDetails: Option[entities.LegalDetails],
    setupType: SetupType,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord {
  def merchantId = id // This way MerchantRecord anywhere a SlickMerchantRecord is expected, ie: Validators

  def legalCountry: Option[entities.Country] =
    legalDetails.flatMap(_.country)
}

case class MerchantUpdate(
    id: Option[UUID],
    active: Option[Boolean],
    businessType: Option[BusinessType],
    businessName: Option[String],
    restaurantType: Option[RestaurantType],
    paymentProcessor: Option[PaymentProcessor],
    paymentProcessorConfig: Option[PaymentProcessorConfig],
    currency: Option[Currency],
    mode: Option[MerchantMode],
    switchMerchantId: Option[UUID],
    setupSteps: Option[Map[MerchantSetupSteps, entities.MerchantSetupStep]],
    setupCompleted: Option[Boolean],
    defaultZoneId: Option[ZoneId],
    features: Option[entities.MerchantFeaturesUpsertion],
    legalDetails: Option[entities.LegalDetailsUpsertion],
    setupType: Option[SetupType],
  ) extends SlickUpdate[MerchantRecord] {
  override def toRecord: MerchantRecord = {
    require(businessType.isDefined, s"Impossible to convert MerchantUpdate without a business type. [$this]")
    require(businessName.isDefined, s"Impossible to convert MerchantUpdate without a business name. [$this]")
    require(restaurantType.isDefined, s"Impossible to convert MerchantUpdate without a restaurant type. [$this]")
    require(mode.isDefined, s"Impossible to convert MerchantUpdate without a mode. [$this]")
    require(paymentProcessor.isDefined, s"Impossible to convert MerchantUpdate without a payment processor. [$this]")
    require(
      paymentProcessorConfig.isDefined,
      s"Impossible to convert MerchantUpdate without a payment processor config. [$this]",
    )
    require(defaultZoneId.isDefined, s"Impossible to convert MerchantUpdate without a default zone id. [$this]")
    require(setupType.isDefined, s"Impossible to convert MerchantUpdate without a setup type. [$this]")

    MerchantRecord(
      id = id.getOrElse(UUID.randomUUID),
      active = active.getOrElse(true),
      businessType = businessType.get,
      businessName = businessName.get,
      restaurantType = restaurantType.get,
      paymentProcessor = paymentProcessor.get,
      paymentProcessorConfig = paymentProcessorConfig.get,
      currency = currency.get,
      mode = mode.get,
      switchMerchantId = switchMerchantId,
      setupSteps = setupSteps,
      setupCompleted = setupCompleted.getOrElse(false),
      loadingStatus = LoadingStatus.NotStarted,
      defaultZoneId = defaultZoneId.get,
      features = mergeFeaturesWithDefault(features, setupType.get.features),
      legalDetails = mergeLegalDetails(legalDetails),
      setupType = setupType.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  override def updateRecord(record: MerchantRecord): MerchantRecord =
    MerchantRecord(
      id = id.getOrElse(record.id),
      active = active.getOrElse(record.active),
      businessType = businessType.getOrElse(record.businessType),
      businessName = businessName.getOrElse(record.businessName),
      restaurantType = restaurantType.getOrElse(record.restaurantType),
      paymentProcessor = paymentProcessor.getOrElse(record.paymentProcessor),
      paymentProcessorConfig = paymentProcessorConfig.getOrElse(record.paymentProcessorConfig),
      currency = currency.getOrElse(record.currency),
      mode = mode.getOrElse(record.mode),
      switchMerchantId = switchMerchantId.orElse(record.switchMerchantId),
      setupSteps = setupSteps.orElse(record.setupSteps),
      setupCompleted = setupCompleted.getOrElse(record.setupCompleted),
      loadingStatus = record.loadingStatus,
      defaultZoneId = defaultZoneId.getOrElse(record.defaultZoneId),
      features = mergeFeaturesWithDefault(features, record.features),
      legalDetails = mergeLegalDetails(legalDetails, record.legalDetails),
      setupType = setupType.getOrElse(record.setupType),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  private def mergeFeaturesWithDefault(
      maybeUpsertion: Option[entities.MerchantFeaturesUpsertion],
      features: entities.MerchantFeatures,
    ): entities.MerchantFeatures =
    maybeUpsertion.fold(features) { upsertion =>
      entities
        .MerchantFeatures
        .create(
          pos = upsertion.pos.getOrElse(features.pos),
          sales = upsertion.sales.getOrElse(features.sales),
          reports = upsertion.reports.getOrElse(features.reports),
          giftCards = upsertion.giftCards.getOrElse(features.giftCards),
          inventory = upsertion.inventory.getOrElse(features.inventory),
          tables = upsertion.tables.getOrElse(features.tables),
          employees = upsertion.employees.getOrElse(features.employees),
          customers = upsertion.customers.getOrElse(features.customers),
          coupons = upsertion.coupons.getOrElse(features.coupons),
          loyalty = upsertion.loyalty.getOrElse(features.loyalty),
          engagement = upsertion.engagement.getOrElse(features.engagement),
          onlineStore = upsertion.onlineStore.getOrElse(features.onlineStore),
        )
    }

  private def mergeLegalDetails(
      maybeUpsertion: Option[entities.LegalDetailsUpsertion],
      details: Option[entities.LegalDetails] = None,
    ): Option[entities.LegalDetails] =
    maybeUpsertion
      .map { upsertion =>
        entities.LegalDetails(
          businessName = upsertion.businessName.orElse(details.flatMap(_.businessName)),
          vatId = upsertion.vatId.orElse(details.flatMap(_.vatId)),
          address = mergeAddress(upsertion.address, details.flatMap(_.address)),
          invoicingCode = upsertion.invoicingCode.orElse(details.flatMap(_.invoicingCode)),
        )
      }
      .orElse(details)

  private def mergeAddress(
      maybeUpsertion: Option[entities.AddressImprovedUpsertion],
      address: Option[entities.AddressImproved] = None,
    ): Option[entities.AddressImproved] =
    maybeUpsertion
      .map { upsertion =>
        entities.AddressImproved(
          stateData = UtilService
            .Geo
            .addressState(
              upsertion.countryCode.map(CountryCode),
              upsertion.stateCode.map(StateCode),
              address.flatMap(_.countryData.map(_.name.pipe(CountryName))),
              address.flatMap(_.stateData.flatMap(_.name.map(StateName))),
            )
            .orElse(address.flatMap(_.stateData)),
          countryData = UtilService
            .Geo
            .country(
              upsertion.countryCode.map(CountryCode),
              address.flatMap(_.countryData.map(_.name.pipe(CountryName))),
            )
            .orElse(address.flatMap(_.countryData)),
          line1 = upsertion.line1.orElse(address.flatMap(_.line1)),
          line2 = upsertion.line2.orElse(address.flatMap(_.line2)),
          city = upsertion.city.orElse(address.flatMap(_.city)),
          postalCode = upsertion.postalCode.orElse(address.flatMap(_.postalCode)),
        )
      }
      .orElse(address)
}
