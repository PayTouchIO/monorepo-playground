package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.LocationPrintReceiptConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.LocationPrintReceiptUpsertion
import io.paytouch.core.data.model.{ LocationPrintReceiptUpdate => LocationPrintReceiptUpdateModel }
import io.paytouch.core.entities.{ LocationPrintReceiptUpdate => LocationPrintReceiptUpdateEntity, _ }
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils._

import scala.concurrent._

class LocationPrintReceiptService(
    val imageUploadService: ImageUploadService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends LocationPrintReceiptConversions {

  protected val dao = daos.locationPrintReceiptDao

  def findAllByLocationIds(
      locationIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, LocationPrintReceipt]] = {
    val locationReceiptsResp = dao.findByLocationIds(locationIds, user.merchantId)
    val imageUploadsResp = imageUploadService.findByObjectIds(locationIds, ImageUploadType.PrintReceipt)
    for {
      locationReceipts <- locationReceiptsResp
      imageUploads <- imageUploadsResp
    } yield fromRecordsToEntities(locationReceipts, imageUploads)
  }

  def convertToUpsertionModel(
      locationId: UUID,
      upsertion: Option[LocationPrintReceiptUpdateEntity],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[LocationPrintReceiptUpsertion]]] =
    upsertion match {
      case Some(ups) =>
        for {
          validReceiptUpdate <- convertToLocationPrintReceiptUpdate(locationId, ups)
          imageUpload <-
            imageUploadService
              .convertToImageUploadUpdates(locationId, ImageUploadType.PrintReceipt, ups.imageUploadId)
        } yield Multiple.combine(validReceiptUpdate, imageUpload)(LocationPrintReceiptUpsertion).map(Some(_))
      case None => Future.successful(Multiple.empty)
    }

  private def convertToLocationPrintReceiptUpdate(
      id: UUID,
      upsertion: LocationPrintReceiptUpdateEntity,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[LocationPrintReceiptUpdateModel]] =
    Future.successful(Multiple.success(fromUpsertionToUpdate(id, upsertion)))
}
