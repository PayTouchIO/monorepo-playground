package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.ImageUploadType

final case class ImageUploadRecord(
    id: UUID,
    merchantId: UUID,
    urls: Option[Map[String, String]],
    fileName: String,
    objectId: Option[UUID],
    objectType: ImageUploadType,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ImageUploadUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    urls: Option[Map[String, String]],
    fileName: Option[String],
    objectId: Option[UUID],
    objectType: Option[ImageUploadType],
  ) extends SlickMerchantUpdate[ImageUploadRecord] {

  def updateRecord(record: ImageUploadRecord): ImageUploadRecord =
    ImageUploadRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      urls = urls.orElse(record.urls),
      fileName = fileName.getOrElse(record.fileName),
      objectId = objectId.orElse(record.objectId),
      objectType = objectType.getOrElse(record.objectType),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: ImageUploadRecord = {
    require(merchantId.isDefined, s"Impossible to convert ImageUploadUpdate without a merchant id. [$this]")
    require(fileName.isDefined, s"Impossible to convert ImageUploadUpdate without a file name. [$this]")
    require(objectType.isDefined, s"Impossible to convert ImageUploadUpdate without an object type. [$this]")
    ImageUploadRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      urls = urls,
      fileName = fileName.get,
      objectId = objectId,
      objectType = objectType.get,
      createdAt = now,
      updatedAt = now,
    )
  }
}
