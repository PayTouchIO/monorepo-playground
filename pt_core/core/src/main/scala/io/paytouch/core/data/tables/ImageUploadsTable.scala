package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.ImageUploadRecord
import io.paytouch.core.data.model.enums.ImageUploadType

class ImageUploadsTable(tag: Tag) extends SlickMerchantTable[ImageUploadRecord](tag, "image_uploads") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def urls = column[Option[Map[String, String]]]("urls")
  def fileName = column[String]("file_name")
  def objectId = column[Option[UUID]]("object_id")
  def objectType = column[ImageUploadType]("object_type")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      urls,
      fileName,
      objectId,
      objectType,
      createdAt,
      updatedAt,
    ).<>(ImageUploadRecord.tupled, ImageUploadRecord.unapply)
}
