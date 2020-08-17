package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.conversions.LocationEmailReceiptConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ LocationEmailReceiptUpdate => LocationEmailReceiptUpdateModel }
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.LocationEmailReceiptUpsertion
import io.paytouch.core.entities.{ LocationEmailReceiptUpdate => LocationEmailReceiptUpdateEntity, _ }
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple._

class LocationEmailReceiptService(
    val imageUploadService: ImageUploadService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends LocationEmailReceiptConversions {
  protected val dao = daos.locationEmailReceiptDao

  def findByLocationId(locationId: UUID)(implicit user: UserContext): Future[Option[LocationEmailReceipt]] =
    findAllByLocationIds(Seq(locationId)).map(_.get(locationId))

  def findAllByLocationIds(
      locationIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, LocationEmailReceipt]] =
    findAllByLocationIds(locationIds, user.merchantId)

  private def findAllByLocationIds(
      locationIds: Seq[UUID],
      merchantId: UUID,
    ): Future[Map[UUID, LocationEmailReceipt]] = {
    val locationReceiptsResp = dao.findByLocationIds(locationIds, merchantId)
    val imageUploadsResp = imageUploadService.findByObjectIds(locationIds, ImageUploadType.EmailReceipt)
    for {
      locationReceipts <- locationReceiptsResp
      imageUploads <- imageUploadsResp
    } yield fromRecordsToEntities(locationReceipts, imageUploads)
  }

  def convertToUpsertionModel(
      locationId: UUID,
      upsertion: Option[LocationEmailReceiptUpdateEntity],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[LocationEmailReceiptUpsertion]]] =
    upsertion match {
      case Some(ups) =>
        for {
          validReceiptUpdate <- convertToLocationEmailReceiptUpdate(locationId, ups)
          imageUpload <-
            imageUploadService
              .convertToImageUploadUpdates(locationId, ImageUploadType.EmailReceipt, ups.imageUploadId)
        } yield Multiple.combine(validReceiptUpdate, imageUpload)(LocationEmailReceiptUpsertion).map(Some(_))

      case None =>
        Future.successful(Multiple.empty)
    }

  private def convertToLocationEmailReceiptUpdate(
      id: UUID,
      upsertion: LocationEmailReceiptUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[LocationEmailReceiptUpdateModel]] =
    Future.successful(Multiple.success(fromUpsertionToUpdate(id, upsertion)))
}
