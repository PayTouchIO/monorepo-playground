package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.LocationReceiptConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.LocationReceiptUpsertion
import io.paytouch.core.data.model.{ LocationReceiptRecord, LocationReceiptUpdate => LocationReceiptUpdateModel }
import io.paytouch.core.entities.{
  ImageUrls,
  Location,
  LocationReceipt,
  LocationUpdate,
  MerchantContext,
  UserContext,
  LocationReceiptUpdate => LocationReceiptUpdateEntity,
}
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils._

import scala.concurrent._

class LocationReceiptService(
    val imageUploadService: ImageUploadService,
    locationService: => LocationService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends LocationReceiptConversions {

  type Record = LocationReceiptRecord
  type Entity = LocationReceipt

  protected val dao = daos.locationReceiptDao

  def convertToLocationReceiptUpdate(
      locationId: UUID,
      update: LocationUpdate,
    )(implicit
      user: UserContext,
    ): Future[LocationReceiptUpdateModel] =
    dao.findByLocationId(locationId = locationId, merchantId = user.merchantId).map {
      case Some(_) => toDefaultLocationReceipt(user.merchantId, locationId)
      case None    => prepopulateLocationReceipt(user.merchantId, locationId, update)
    }

  def findAllByLocationIds(locationIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, LocationReceipt]] =
    for {
      records <- dao.findByLocationIds(locationIds, user.merchantId)
      entities <- enrich(records)(user.toMerchantContext)
      entitiesPerLocation = entities.map(e => e.locationId -> e).toMap
    } yield entitiesPerLocation

  def findByLocationId(locationId: UUID)(implicit user: UserContext): Future[Option[LocationReceipt]] =
    findAllByLocationIds(Seq(locationId)).map(_.get(locationId))

  def findFirstByMerchantId(merchantId: UUID)(implicit merchant: MerchantContext): Future[Option[Entity]] =
    dao.findByMerchantId(merchantId).flatMap(enrich(_).map(_.headOption))

  private def enrich(record: Record)(implicit merchant: MerchantContext): Future[Entity] =
    enrich(Seq(record)).map(_.head)

  private def enrich(records: Seq[Record])(implicit merchant: MerchantContext): Future[Seq[Entity]] = {
    val locationsPerReceiptR = getLocationPerReceipt(records)
    val emailImageUrlsPerReceiptR = getImageUrlsPerReceipt(records, ImageUploadType.EmailReceipt)
    val printImageUrlsPerReceiptR = getImageUrlsPerReceipt(records, ImageUploadType.PrintReceipt)
    for {
      locationsPerReceipt <- locationsPerReceiptR
      emailImageUrlsPerReceipt <- emailImageUrlsPerReceiptR
      printImageUrlsPerReceipt <- printImageUrlsPerReceiptR
    } yield fromRecordsAndOptionsToEntities(
      records,
      locationsPerReceipt,
      emailImageUrlsPerReceipt,
      printImageUrlsPerReceipt,
    )
  }

  private def getLocationPerReceipt(
      records: Seq[Record],
    )(implicit
      merchant: MerchantContext,
    ): Future[Map[Record, Location]] = {
    val locationIds = records.map(_.locationId)
    locationService.findByIdsWithMerchantContext(locationIds).map { locations =>
      records.flatMap(record => locations.find(_.id == record.locationId).map(l => record -> l)).toMap
    }
  }

  private def getImageUrlsPerReceipt(
      records: Seq[Record],
      imageUploadType: ImageUploadType,
    ): Future[Map[Record, Seq[ImageUrls]]] = {
    val ids = records.map(_.locationId)
    imageUploadService.findByObjectIds(ids, imageUploadType).map(_.mapKeysToObjs(records)(_.locationId))
  }

  def convertToUpsertionModel(
      locationId: UUID,
      upsertion: Option[LocationReceiptUpdateEntity],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[LocationReceiptUpsertion]]] =
    upsertion match {
      case Some(update) =>
        for {
          validReceiptUpdate <- convertToLocationReceiptUpdate(locationId, update)
          emailImageUpload <- imageUploadService.convertToImageUploadUpdates(
            locationId,
            ImageUploadType.EmailReceipt,
            update.emailImageUploadIds,
          )
          printImageUpload <- imageUploadService.convertToImageUploadUpdates(
            locationId,
            ImageUploadType.PrintReceipt,
            update.printImageUploadIds,
          )
        } yield Multiple
          .combine(validReceiptUpdate, emailImageUpload, printImageUpload)(LocationReceiptUpsertion)
          .map(Some(_))
      case None => Future.successful(Multiple.empty)
    }

  private def convertToLocationReceiptUpdate(
      id: UUID,
      upsertion: LocationReceiptUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[LocationReceiptUpdateModel]] =
    Future.successful(Multiple.success(fromUpsertionToUpdate(id, upsertion)))

}
