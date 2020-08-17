package io.paytouch.core.services

import java.time.ZoneId
import java.util.UUID

import scala.concurrent._

import akka.actor._

import cats.data._
import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core._
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.conversions.LocationConversions
import io.paytouch.core.data._
import io.paytouch.core.data.daos._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.MerchantMode
import io.paytouch.core.data.model.upsertions.LocationUpsertion
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.errors._
import io.paytouch.core.expansions.LocationExpansions
import io.paytouch.core.filters.LocationFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.LocationValidator

class LocationService(
    val customerLocationService: CustomerLocationService,
    val eventTracker: ActorRef withTag EventTracker,
    val locationAvailabilityService: LocationAvailabilityService,
    val locationSettingsService: LocationSettingsService,
    val locationEmailReceiptService: LocationEmailReceiptService,
    val locationPrintReceiptService: LocationPrintReceiptService,
    val locationReceiptService: LocationReceiptService,
    val loyaltyProgramLocationService: LoyaltyProgramLocationService,
    val setupStepService: SetupStepService,
    val taxRateService: TaxRateService,
    val taxRateLocationService: TaxRateLocationService,
    val userLocationService: UserLocationService,
    val userService: UserService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends LocationConversions
       with CreateAndUpdateFeatureWithStateProcessing
       with FindAllFeature
       with FindByIdFeature
       with SoftDeleteFeature {
  type Creation = LocationCreation
  type Dao = LocationDao
  type Entity = Location
  type Expansions = LocationExpansions
  type Filters = LocationFilters
  type Model = LocationUpsertion
  type Record = LocationRecord
  type State = Unit
  type Update = entities.LocationUpdate
  type Validator = LocationValidator

  protected val dao = daos.locationDao
  protected val validator = new LocationValidator
  val defaultFilters = LocationFilters()

  val classShortName = ExposedName.Location

  protected def convertToUpsertionModel(locationId: UUID, update: Update)(implicit user: UserContext) =
    convertToLocationUpdate(locationId, update).flatMapTraverse { location =>
      val locationReceiptR = locationReceiptService.convertToLocationReceiptUpdate(locationId, update)
      val locationSettingsR = locationSettingsService.defaultLocationSettings(locationId)
      val userLocationsR = convertToUserLocationUpdates(locationId)
      for {
        locationReceipt <- locationReceiptR
        locationSettings <- locationSettingsR
        locationEmailReceipt = locationEmailReceiptService.defaultLocationEmailReceipt(locationId)
        locationPrintReceipt = locationPrintReceiptService.defaultLocationPrintReceipt(locationId)
        availabilities = locationAvailabilityService.toAvailabilities(Some(locationId), update.openingHours)
        userLocations <- userLocationsR
      } yield LocationUpsertion(
        location,
        locationSettings,
        locationEmailReceipt,
        locationPrintReceipt,
        locationReceipt,
        availabilities,
        userLocations,
        update.initialOrderNumber,
      )
    }

  private def convertToLocationUpdate(
      id: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[Multiple.ErrorsOr[model.LocationUpdate]] =
    validator.validateEmailFormat(update.email).mapNested { validEmail =>
      fromUpsertionToUpdate(id, update.copy(email = validEmail))
    }

  private def convertToUserLocationUpdates(
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): Future[Seq[UserLocationUpdate]] =
    userService.findOwners(user.merchantId).map { owners =>
      val userIds = (owners.map(_.id) :+ user.id).distinct
      userIds.map(userId => userLocationService.toUserLocationUpdate(userId = userId, locationId = locationId))
    }

  def enrich(records: Seq[Record], filters: Filters)(expansions: Expansions)(implicit user: UserContext) = {
    implicit val merchant = user.toMerchantContext
    for {
      settings <- getOptionalLocationSettings(records)(expansions.withSettings)
      taxRates <- getOptionalTaxRates(records)(expansions.withTaxRates)
      openingHours <- getOptionalAvailabilitiesPerLocation(records)(expansions.withOpeningHours)
    } yield fromRecordsAndOptionsToEntities(records, settings, taxRates, openingHours)
  }

  private def getOptionalLocationSettings(
      locations: Seq[Record],
    )(
      withSettings: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[LocationSettings]] =
    if (withSettings) locationSettingsService.findAllByLocationIds(locations.map(_.id)).map { locationSettings =>
      Some(groupLocationSettingsPerLocation(locationSettings, locations))
    }
    else Future.successful(None)

  private def getOptionalTaxRates(
      locations: Seq[Record],
    )(
      withTaxRates: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataSeqByRecord[TaxRate]] =
    if (withTaxRates) taxRateService.findByLocationIds(locations).map(Some(_))
    else Future.successful(None)

  private def getOptionalAvailabilitiesPerLocation(
      locations: Seq[Record],
    )(
      withOpeningHours: Boolean,
    )(implicit
      user: UserContext,
    ): Future[DataByRecord[Availabilities]] =
    if (withOpeningHours) {
      val locationIds = locations.map(_.id)
      locationAvailabilityService.findAllPerLocation(locationIds).map { recordsByItemIds =>
        Some(recordsByItemIds.mapKeysToRecords(locations))
      }
    }
    else Future.successful(None)

  def findAllByCustomerIds(customerIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, Seq[Entity]]] =
    for {
      customerLocations <- customerLocationService.findAllByCustomerIds(customerIds)
      locations <- findByIds(customerLocations.map(_.locationId))
    } yield groupLocationsPerItemId(customerLocations, locations)

  def findAllByLoyaltyProgramIds(
      loyaltyProgramIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[Entity]]] =
    for {
      loyaltyProgramLocations <- loyaltyProgramLocationService.findAllByLoyaltyProgramIds(loyaltyProgramIds)
      locations <- findByIds(loyaltyProgramLocations.map(_.locationId))
    } yield groupLocationsPerItemId(loyaltyProgramLocations, locations)

  def findAllByUserIds(
      userIds: Seq[UUID],
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[Entity]]] =
    dao.findAllByUserIds(userIds).flatMap { locationsPerUser =>
      val allRecords: Seq[Record] = locationsPerUser.values.flatten.toSeq.distinct
      enrich(allRecords, defaultFilters)(expansions).map { entities =>
        locationsPerUser.transform {
          case (_, records) =>
            val recordIds = records.map(_.id)
            entities.filter(e => recordIds.contains(e.id))
        }
      }
    }

  def findByIds(locationIds: Seq[UUID])(implicit user: UserContext): Future[Seq[Entity]] =
    findByIdsWithMerchantContext(locationIds)(user.toMerchantContext)

  def findByIdsWithMerchantContext(locationIds: Seq[UUID])(implicit merchant: MerchantContext): Future[Seq[Entity]] =
    dao.findByIds(locationIds).map(records => fromRecordsToEntities(records))

  def findByMerchantIds(merchantIds: Seq[UUID]): Future[Map[UUID, Seq[Record]]] =
    dao.findAllByMerchantIds(merchantIds)

  def findAll(implicit merchant: MerchantContext): Future[Seq[Entity]] =
    dao.findAllByMerchantId(merchant.id).map(records => fromRecordsToEntities(records))

  def updateSettings(
      locationId: UUID,
      settingsUpdate: entities.LocationSettingsUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Result[LocationSettings]]] =
    locationSettingsService
      .update(locationId, settingsUpdate)
      .flatMapTraverse { result =>
        setupStepService
          .simpleCheckStepCompletion(user.merchantId, MerchantSetupSteps.DesignReceipts)
          .as(result)
      }

  protected def saveCreationState(id: UUID, creation: Creation)(implicit user: UserContext): Future[Option[State]] =
    Future.successful(None)

  protected def saveCurrentState(record: Record)(implicit user: UserContext): Future[State] =
    Future.unit

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    setupStepService
      .checkStepCompletion(
        entity,
        MerchantSetupSteps.SetupLocations,
      )

  def createDefaultLocation(
      merchant: MerchantRecord,
      userOwner: UserRecord,
      merchantCreation: MerchantCreation,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Location]]] =
    merchant.mode match {
      case MerchantMode.Demo =>
        Future.successful(Multiple.empty)

      case MerchantMode.Production =>
        create(
          id = UUID.randomUUID,
          creation = convertToDefaultLocationCreation(merchant, userOwner, merchantCreation),
        ).mapNested {
          case (_, location) => Some(location)
        }
    }

  def findFirstLocation(implicit merchant: MerchantContext): Future[Option[Entity]] =
    dao.findFirstByMerchantId(merchant.id).map(_.map(fromRecordToEntity))

  def findTimezoneForLocationWithFallback(
      locationId: Option[UUID],
    )(implicit
      merchant: MerchantContext,
    ): Future[ZoneId] =
    dao
      .findByIds(locationId.toSeq)
      .map(records =>
        fromRecordsToEntities(records)
          .map(_.timezone)
          .headOption
          .getOrElse(ZoneId.of("UTC")),
      )

  def deepCopy(
      from: UUID,
      to: UUID,
    )(implicit
      user: UserContext,
    ): Future[Multiple.ErrorsOr[Unit]] =
    (for {
      a <- validator.validateBelongsToMerchant(Seq(from))
      b <-
        validator
          .validateBelongsToMerchant(Seq(to))
          .map(_.leftMap(_ => TargetLocationDoesNotBelongToSameMerchant(user.merchantId).pure[Nel]))
    } yield (a, b).tupled).flatMapTraverse(_ => dao.deepCopy(from, to))
}
