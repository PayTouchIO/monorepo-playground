package io.paytouch.core.conversions

import awscala.s3.Bucket
import java.util.UUID

import io.paytouch.core.{ CloudfrontImagesDistribution, S3ImagesBucket }
import io.paytouch.core.data.model
import io.paytouch.core.entities
import io.paytouch.core.entities.{ ImageUrls, UserContext }
import io.paytouch.utils.Tagging._

trait ImageUploadConversions
    extends EntityConversion[model.ImageUploadRecord, entities.ImageUpload]
       with ModelConversion[entities.ImageUploadUpsertion, model.ImageUploadUpdate] {

  type Record <: model.ImageUploadRecord

  def fromRecordToEntity(
      record: Record,
      s3ToCloudFrontConverter: String => String,
    )(implicit
      user: UserContext,
    ): entities.ImageUpload =
    entities.ImageUpload(
      id = record.id,
      urls = record.urls.map(_.transform((_, v) => s3ToCloudFrontConverter(v))),
      fileName = record.fileName,
      objectId = record.objectId,
      objectType = record.objectType,
      uploadUrl = None,
    )

  def fromRecordToImageUrlsEntity(
      record: Record,
      s3ToCloudFrontConverter: String => String,
    ): ImageUrls =
    ImageUrls(
      imageUploadId = record.id,
      urls = record
        .urls
        .getOrElse(Map.empty)
        .transform((_, v) => s3ToCloudFrontConverter(v)),
    )

  def fromUpsertionV1ToUpdate(
      id: UUID,
      upsertion: entities.ImageUploadUpsertionV1,
    )(implicit
      user: UserContext,
    ): model.ImageUploadUpdate =
    model.ImageUploadUpdate(
      id = Some(id),
      merchantId = Some(user.merchantId),
      urls = None,
      fileName = Some(upsertion.originalFileName),
      objectId = None,
      objectType = Some(upsertion.objectType),
    )

  def fromUpsertionToUpdate(
      id: UUID,
      upsertion: entities.ImageUploadUpsertion,
    )(implicit
      user: UserContext,
    ): model.ImageUploadUpdate =
    model.ImageUploadUpdate(
      id = Some(id),
      merchantId = Some(user.merchantId),
      urls = None,
      fileName = upsertion.originalFileName,
      objectId = None,
      objectType = upsertion.objectType,
    )

  def toImageUploadUpdates(imageUploadIds: Seq[UUID], objectId: UUID): Seq[model.ImageUploadUpdate] =
    imageUploadIds.map(toImageUploadUpdate(_, objectId))

  def toImageUploadUpdate(imageUploadId: UUID, objectId: UUID): model.ImageUploadUpdate =
    model.ImageUploadUpdate(
      id = Some(imageUploadId),
      merchantId = None,
      urls = None,
      fileName = None,
      objectId = Some(objectId),
      objectType = None,
    )

  def fromRecordsAndOptionsToEntities(
      records: Seq[Record],
      imageUrlsPerRecord: Map[Record, Map[String, String]],
      uploadUrlsPerRecord: Map[Record, String],
    ) =
    records.map { record =>
      val imageUrls = imageUrlsPerRecord.get(record)
      val uploadUrl = uploadUrlsPerRecord.get(record)
      fromRecordAndOptionsToEntity(record, imageUrls, uploadUrl)
    }

  def fromRecordAndOptionsToEntity(
      record: Record,
      imageUrls: Option[Map[String, String]],
      uploadUrl: Option[String],
    ) =
    entities.ImageUpload(
      id = record.id,
      urls = imageUrls,
      fileName = record.fileName,
      objectId = record.objectId,
      objectType = record.objectType,
      uploadUrl = uploadUrl,
    )
}
