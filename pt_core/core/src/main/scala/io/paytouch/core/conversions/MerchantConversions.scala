package io.paytouch.core.conversions

import java.util.UUID

import cats.implicits._

import io.paytouch.core._
import io.paytouch.core.data._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.PaymentProcessorConfig.PaytouchConfig
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.services._

trait MerchantConversions {
  def userService: UserService
  def locationService: LocationService

  def fromMerchantRecordsToEntities(
      records: Seq[MerchantRecord],
      firstLocationReceiptPerMerchant: MerchantRecord => Option[LocationReceipt],
      setupStepsPerMerchant: MerchantRecord => Option[Map[MerchantSetupSteps, MerchantSetupStatus]],
      userOwnerPerMerchant: MerchantRecord => Option[UserRecord],
      locationsPerMerchant: MerchantRecord => Option[Seq[LocationRecord]],
      legalDetailsPerMerchant: MerchantRecord => Option[LegalDetails],
    ): Seq[Merchant] =
    records.map { record =>
      implicit val merchantContext = MerchantContext.extract(record)

      fromRecordAndOptionToEntity(
        record,
        firstLocationReceipt = firstLocationReceiptPerMerchant(record),
        ownerUser = userOwnerPerMerchant(record).map(userService.fromRecordToEntity),
        locations = locationsPerMerchant(record).map(_.map(locationService.fromRecordToEntity)),
        legalDetails = legalDetailsPerMerchant(record),
        setupSteps = setupStepsPerMerchant(record),
      )
    }

  def fromMerchantAndUserRecordToEntity(record: MerchantRecord, ownerUser: UserRecord): Merchant = {
    implicit val merchantContext = MerchantContext.extract(record)
    fromRecordAndOptionToEntity(
      record,
      ownerUser = Some(userService.fromRecordToEntity(ownerUser)),
    )
  }

  def fromRecordAndOptionToEntity(
      record: MerchantRecord,
      firstLocationReceipt: Option[LocationReceipt] = None,
      ownerUser: Option[User] = None,
      locations: Option[Seq[Location]] = None,
      legalDetails: Option[LegalDetails] = None,
      setupSteps: Option[Map[MerchantSetupSteps, MerchantSetupStatus]] = None,
    ): Merchant =
    Merchant(
      id = record.id,
      businessType = record.businessType,
      restaurantType = record.restaurantType,
      currency = record.currency,
      paymentProcessor = record.paymentProcessor,
      paymentProcessorConfig = record.paymentProcessorConfig,
      name = record.businessName,
      displayName = record.businessName,
      logoUrls = firstLocationReceipt.toSeq.flatMap(_.emailImageUrls),
      ownerUser = ownerUser,
      locations = locations,
      mode = record.mode,
      loadingStatus = record.loadingStatus,
      defaultZoneId = record.defaultZoneId,
      setupSteps = setupSteps,
      setupCompleted = record.setupCompleted,
      features = MerchantConversions.mergeFeatures(record.setupType, record.features),
      legalDetails = legalDetails,
      legalCountry = legalCountry(legalDetails),
      setupType = record.setupType,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def legalCountry(legalDetails: Option[LegalDetails]): Country =
    legalDetails.flatMap(_.country).getOrElse(UtilService.Geo.UnitedStates)

  def fromMerchantCreationToUpdate(id: UUID, creation: MerchantCreation): model.MerchantUpdate =
    model.MerchantUpdate(
      id = id.some,
      active = None,
      businessType = creation.businessType.some,
      businessName = creation.businessName.some,
      restaurantType = creation.restaurantType.some,
      paymentProcessor = PaymentProcessor.Paytouch.some,
      paymentProcessorConfig = PaytouchConfig().some,
      currency = creation.currency.some,
      mode = creation.mode.some,
      switchMerchantId = None,
      setupSteps = None,
      setupCompleted = None,
      legalDetails = creation.legalDetails,
      defaultZoneId = creation.zoneId.some,
      features = creation.features,
      setupType = creation.setupType.some,
    )

  def fromAdminMerchantUpdateEntityToUpdateModel(id: UUID, update: AdminMerchantUpdate): model.MerchantUpdate =
    model.MerchantUpdate(
      id = Some(id),
      active = None,
      businessType = update.businessType,
      businessName = update.businessName,
      restaurantType = update.restaurantType,
      paymentProcessor = None,
      paymentProcessorConfig = update.worldpay,
      currency = update.currency,
      mode = None,
      switchMerchantId = None,
      setupSteps = None,
      setupCompleted = None,
      defaultZoneId = update.zoneId,
      features = update.features,
      legalDetails = update.legalDetails,
      setupType = update.setupType,
    )

  def fromApiMerchantUpdateEntityToUpdateModel(id: UUID, update: ApiMerchantUpdate): model.MerchantUpdate =
    model.MerchantUpdate(
      id = Some(id),
      active = None,
      businessType = None,
      businessName = update.businessName,
      restaurantType = update.restaurantType,
      paymentProcessor = None,
      paymentProcessorConfig = None,
      currency = None,
      mode = None,
      switchMerchantId = None,
      setupSteps = None,
      setupCompleted = None,
      defaultZoneId = update.zoneId,
      features = None,
      legalDetails = update.legalDetails,
      setupType = None,
    )

  def inferMerchantCreation(
      merchant: MerchantRecord,
      owner: UserRecord,
      mode: MerchantMode,
    ) =
    MerchantCreation(
      businessType = merchant.businessType,
      businessName = merchant.businessName,
      restaurantType = merchant.restaurantType,
      currency = merchant.currency,
      firstName = owner.firstName,
      lastName = owner.lastName,
      password = s"temp-${UUID.randomUUID}", // will be changed in insertion
      email = owner.email,
      zoneId = merchant.defaultZoneId,
      pin = None,
      mode = mode,
      setupType = merchant.setupType,
    )

  protected def inferUserContext(
      merchant: MerchantRecord,
      user: UserRecord,
      source: ContextSource,
    ): UserContext =
    UserContext(
      id = user.id,
      merchantId = merchant.id,
      currency = merchant.currency,
      businessType = merchant.businessType,
      locationIds = Seq.empty,
      adminId = None,
      merchantSetupCompleted = false,
      source = source,
      paymentProcessor = merchant.paymentProcessor,
    )
}

object MerchantConversions {
  def mergeFeatures(setupType: SetupType, recordFeatures: MerchantFeatures) = setupType.features ++ recordFeatures
}
