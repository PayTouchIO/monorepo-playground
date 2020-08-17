package io.paytouch.ordering.services

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.ImageUrls
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.model.SlickRecord
import io.paytouch.ordering.entities.{ ExposedEntity, UserContext }
import io.paytouch.ordering.messages.SQSMessageHandler

import scala.concurrent.{ ExecutionContext, Future }

class ImageService(messageHandler: SQSMessageHandler)(implicit val ec: ExecutionContext, val daos: Daos) {

  def notifyNewImageIds[E <: ExposedEntity, R <: SlickRecord](
      id: UUID,
      e: E,
      optR: Option[R],
    )(
      fe: E => Seq[ImageUrls],
      fr: R => Seq[ImageUrls],
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    Future {
      val (currentImageIds, previousImageIds) = extractImageIds(e, optR)(fe, fr)
      val newImageIds = currentImageIds diff previousImageIds
      messageHandler.sendImageIdsAssociated(id, newImageIds)
    }

  def notifyDeletedImageIds[E <: ExposedEntity, R <: SlickRecord](
      e: E,
      optR: Option[R],
    )(
      fe: E => Seq[ImageUrls],
      fr: R => Seq[ImageUrls],
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    Future {
      val (currentImageIds, previousImageIds) = extractImageIds(e, optR)(fe, fr)
      val deletedImageIds = previousImageIds diff currentImageIds
      messageHandler.sendImagesDeleted(deletedImageIds)
    }

  private def extractImageIds[E <: ExposedEntity, R <: SlickRecord](
      e: E,
      optR: Option[R],
    )(
      fe: E => Seq[ImageUrls],
      fr: R => Seq[ImageUrls],
    ): (Seq[UUID], Seq[UUID]) = {
    val currentImageIds = fe(e).map(_.imageUploadId)
    val previousImageIds = optR.map(r => fr(r).map(_.imageUploadId)).getOrElse(Seq.empty)
    (currentImageIds, previousImageIds)
  }

}
