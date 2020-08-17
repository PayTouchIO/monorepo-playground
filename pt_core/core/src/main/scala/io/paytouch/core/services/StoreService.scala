package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.StoreConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.Store

import scala.concurrent._

class StoreService(imageUploadService: ImageUploadService)(implicit val ec: ExecutionContext, val daos: Daos)
    extends StoreConversions {

  def findByLocationId(locationId: UUID): Future[Option[Store]] =
    imageUploadService
      .findByObjectId(locationId, ImageUploadType.StoreLogo)
      .map(logoUrls => Some(toEntity(locationId, logoUrls)))

  def findByOptLocationId(maybeId: Option[UUID]): Future[Option[Store]] =
    maybeId.fold[Future[Option[Store]]](Future.successful(None))(findByLocationId)
}
