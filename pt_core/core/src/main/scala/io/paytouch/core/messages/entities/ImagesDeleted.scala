package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class ImagesDeleted(eventName: String, payload: ImagesDeletion) extends PtCoreMsg[ImageIds]

object ImagesDeleted {

  val eventName = "images_deleted"

  def apply(imageIds: Seq[UUID], merchantId: UUID): ImagesDeleted =
    ImagesDeleted(eventName, ImagesDeletion(imageIds, merchantId))
}

final case class ImagesDeletion(
    `object`: ExposedName,
    merchantId: UUID,
    data: ImageIds,
  ) extends EntityPayloadLike[ImageIds]

object ImagesDeletion {
  def apply(imageIds: Seq[UUID], merchantId: UUID): ImagesDeletion =
    ImagesDeletion(ExposedName.ImageUpload, merchantId, ImageIds(imageIds))
}

final case class ImageIds(ids: Seq[UUID])
