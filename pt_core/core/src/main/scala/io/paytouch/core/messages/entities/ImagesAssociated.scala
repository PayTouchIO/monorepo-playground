package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class ImagesAssociated(eventName: String, payload: ImagesAssociationPayload)
    extends PtCoreMsg[ImagesAssociation]

object ImagesAssociated {

  val eventName = "images_associated"

  def apply(
      objectId: UUID,
      imageIds: Seq[UUID],
      merchantId: UUID,
    ): ImagesAssociated = {
    val payload = ImagesAssociationPayload(objectId = objectId, imageIds = imageIds, merchantId = merchantId)
    ImagesAssociated(eventName, payload)
  }
}

final case class ImagesAssociationPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: ImagesAssociation,
  ) extends EntityPayloadLike[ImagesAssociation]

object ImagesAssociationPayload {
  def apply(
      objectId: UUID,
      imageIds: Seq[UUID],
      merchantId: UUID,
    ): ImagesAssociationPayload =
    ImagesAssociationPayload(ExposedName.ImageUpload, merchantId, ImagesAssociation(objectId, imageIds))
}

final case class ImagesAssociation(objectId: UUID, imageIds: Seq[UUID])
