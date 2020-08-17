package io.paytouch.core.processors

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.messages.entities.{ ImagesAssociated, ImagesDeleted, SQSMessage }
import io.paytouch.core.services.ImageUploadService

class ImagesProcessor(imageUploadService: ImageUploadService)(implicit val ec: ExecutionContext, val daos: Daos)
    extends Processor {
  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: ImagesDeleted    => processImagesDeleted(msg)
    case msg: ImagesAssociated => processImagesAssociated(msg)
  }

  private def processImagesDeleted(msg: ImagesDeleted): Future[Unit] = {
    val ids = msg.payload.data.ids
    val merchantId = msg.payload.merchantId
    imageUploadService
      .deleteImageByIdsAndMerchantId(ids, merchantId)
      .void
  }

  private def processImagesAssociated(msg: ImagesAssociated): Future[Unit] = {
    val merchantId = msg.payload.merchantId
    val imageIds = msg.payload.data.imageIds
    val objectId = msg.payload.data.objectId
    imageUploadService
      .associateImagesToObjectId(imageIds = imageIds, objectId = objectId, merchantId = merchantId)
      .void
  }

}
