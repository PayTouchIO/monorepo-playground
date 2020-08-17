package io.paytouch.core.services

import java.util.UUID

import akka.actor.ActorRef

import scala.concurrent._

import cats.implicits._

import io.paytouch.core._
import io.paytouch.core.async.monitors._
import io.paytouch.core.conversions.LocationSettingsConversions
import io.paytouch.core.data._
import io.paytouch.core.data.daos.{ Daos, LocationSettingsDao }
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.NoFilters
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.services.features._
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.Multiple
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.LocationSettingsValidator

class LocationSettingsService(
    val imageUploadService: ImageUploadService,
    val locationEmailReceiptService: LocationEmailReceiptService,
    val locationPrintReceiptService: LocationPrintReceiptService,
    val locationReceiptService: LocationReceiptService,
    val monitor: ActorRef withTag LocationSettingsMonitor,
    val messageHandler: SQSMessageHandler,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends LocationSettingsConversions
       with UpdateFeatureWithStateProcessing
       with EnrichFeature {

  type Dao = LocationSettingsDao
  type Entity = LocationSettings
  type Expansions = NoExpansions
  type Model = model.upsertions.LocationSettingsUpsertion
  type Filters = NoFilters
  type Record = LocationSettingsRecord
  type Update = entities.LocationSettingsUpdate
  type Validator = LocationSettingsValidator

  type State = (LocationSettingsRecord, Seq[ImageUploadRecord])

  val defaultFilters = NoFilters()
  val defaultExpansions = NoExpansions()

  protected val dao = daos.locationSettingsDao
  protected val validator = new LocationSettingsValidator

  val locationEmailReceiptDao = daos.locationEmailReceiptDao
  val locationPrintReceiptDao = daos.locationPrintReceiptDao
  val imageUploadDao = daos.imageUploadDao

  def findAllByLocationIds(locationIds: Seq[UUID])(implicit user: UserContext): Future[Seq[Entity]] = {
    val locationSettingsResp = dao.findByLocationIds(locationIds, user.merchantId)
    val locationEmailReceiptsResp = locationEmailReceiptService.findAllByLocationIds(locationIds)
    val locationPrintReceiptsResp = locationPrintReceiptService.findAllByLocationIds(locationIds)
    val locationReceiptsResp = locationReceiptService.findAllByLocationIds(locationIds)
    val splashImageUrlsResp = imageUploadService.findByObjectIds(locationIds, ImageUploadType.CfdSplashScreen)
    for {
      locationSettings <- locationSettingsResp
      locationEmailReceipts <- locationEmailReceiptsResp
      locationPrintReceipts <- locationPrintReceiptsResp
      locationReceipts <- locationReceiptsResp
      splashImageUrls <- splashImageUrlsResp
    } yield fromRecordsAndOptionsToEntities(
      locationSettings,
      locationEmailReceipts,
      locationPrintReceipts,
      locationReceipts,
      splashImageUrls,
    )
  }

  def enrich(records: Seq[Record], filters: Filters)(e: Expansions)(implicit user: UserContext): Future[Seq[Entity]] = {
    val locationIds = records.map(_.locationId)
    val locationSettingsResp = dao.findByLocationIds(locationIds, user.merchantId)
    val locationEmailReceiptsResp = locationEmailReceiptService.findAllByLocationIds(locationIds)
    val locationPrintReceiptsResp = locationPrintReceiptService.findAllByLocationIds(locationIds)
    val locationReceiptsResp = locationReceiptService.findAllByLocationIds(locationIds)
    val splashImageUrlsResp = imageUploadService.findByObjectIds(locationIds, ImageUploadType.CfdSplashScreen)
    for {
      locationSettings <- locationSettingsResp
      locationEmailReceipts <- locationEmailReceiptsResp
      locationPrintReceipts <- locationPrintReceiptsResp
      locationReceipts <- locationReceiptsResp
      splashImageUrls <- splashImageUrlsResp
    } yield fromRecordsAndOptionsToEntities(
      locationSettings,
      locationEmailReceipts,
      locationPrintReceipts,
      locationReceipts,
      splashImageUrls,
    )
  }

  def defaultLocationSettings(
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): Future[Option[model.LocationSettingsUpdate]] =
    dao.findByLocationId(locationId, user.merchantId).flatMap {
      case Some(_) => Future.successful(None)
      case None    => dao.findLatestSettings(user.merchantId).map(s => Some(toDefaultLocationSettings(locationId, s)))
    }

  def convertToUpsertionModel(
      locationId: UUID,
      update: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      settingsUpdate <- convertToLocationSettingsUpdate(locationId, update)
      emailReceiptUpsert <- locationEmailReceiptService.convertToUpsertionModel(locationId, update.locationEmailReceipt)
      printReceiptUpdate <- locationPrintReceiptService.convertToUpsertionModel(locationId, update.locationPrintReceipt)
      receiptUpdate <- locationReceiptService.convertToUpsertionModel(locationId, update.locationReceipt)
      cfdSplashImageUploads <- convertToImageUploadUpdates(locationId, update)
    } yield Multiple.combine(
      settingsUpdate,
      emailReceiptUpsert,
      printReceiptUpdate,
      receiptUpdate,
      cfdSplashImageUploads,
    )(model.upsertions.LocationSettingsUpsertion)

  def convertToLocationSettingsUpdate(
      locationId: UUID,
      upsertion: Update,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[model.LocationSettingsUpdate]] =
    Multiple.success(fromUpsertionToUpdate(locationId, upsertion)).pure[Future]

  protected def convertToImageUploadUpdates(locationId: UUID, update: Update)(implicit user: UserContext) =
    imageUploadService.convertToImageUploadUpdates(
      itemId = locationId,
      imageUploadType = ImageUploadType.CfdSplashScreen,
      imageUploadIds = update.cfd.flatMap(_.splashImageUploadIds),
    )

  protected def saveCurrentState(record: Record)(implicit user: UserContext): Future[State] =
    for {
      imageUploads <- imageUploadDao.findByObjectIds(Seq(record.locationId))
    } yield (record, imageUploads)

  override def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Entity)] =
    for {
      (resultType, record) <- f
      entity <- enrich(record, defaultFilters)(defaultExpansions)
    } yield (resultType, entity)

  override protected def processChangeOfState(
      state: Option[State],
      update: entities.LocationSettingsUpdate,
      resultType: ResultType,
      entity: LocationSettings,
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    state
      .map {
        case s @ (locationSettingsRecord, _) =>
          monitor ! LocationSettingsChange(s, update, user)

          messageHandler.sendLocationSettingsUpdatedMsg(locationSettingsRecord.locationId)
      }
      .pure[Future]
      .void

  def updateOnlineStorefrontActive(merchantId: UUID, items: Seq[UpdateActiveItem]): Future[Unit] =
    dao.bulkUpdateOnlineStorefrontEnabled(merchantId, items)

  def updateRapidoActive(merchantId: UUID, items: Seq[UpdateActiveItem]): Future[Unit] =
    dao.bulkUpdateRapidoEnabled(merchantId, items)

  def setOnlineStorefrontEnabled(merchantId: UUID, locationId: UUID): Future[Unit] =
    dao.setOnlineStorefrontEnabled(merchantId, locationId).void

  def setDeliveryProvidersEnabled(merchantId: UUID, locationId: UUID): Future[Unit] =
    dao.setDeliveryProvidersEnabled(merchantId, locationId).void
}
