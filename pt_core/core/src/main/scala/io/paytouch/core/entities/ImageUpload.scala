package io.paytouch.core.entities

import java.io.File
import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.entities.enums.ExposedName

final case class ImageUpload(
    id: UUID,
    urls: Option[Map[String, String]],
    fileName: String,
    objectId: Option[UUID],
    objectType: ImageUploadType,
    uploadUrl: Option[String],
  ) extends ExposedEntity {
  val classShortName = ExposedName.ImageUpload
}

final case class ImageUploadUpsertionV1(
    objectType: ImageUploadType,
    file: File,
    originalFileName: String,
  )

final case class ImageUploadCreation(
    objectType: ImageUploadType,
    originalFileName: String,
  ) extends CreationEntity[ImageUpload, ImageUploadUpsertion] {
  def asUpdate = ImageUploadUpsertion(objectType = objectType.some, originalFileName = originalFileName.some)
}

final case class ImageUploadUpsertion(
    objectType: Option[ImageUploadType],
    originalFileName: Option[String],
  ) extends UpdateEntity[ImageUpload]
