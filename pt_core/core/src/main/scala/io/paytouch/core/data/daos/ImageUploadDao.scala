package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickMerchantDao, SlickUpsertDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ImageUploadRecord, ImageUploadUpdate }
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.tables.ImageUploadsTable
import io.paytouch.core.utils.UtcTime

class ImageUploadDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao with SlickUpsertDao {
  type Record = ImageUploadRecord
  type Upsertion = ImageUploadUpdate
  type Update = ImageUploadUpdate
  type Table = ImageUploadsTable

  val table = TableQuery[Table]

  def updateUrls(id: UUID, imageUrls: Map[String, String]): Future[Boolean] =
    table
      .withFilter(_.id === id)
      .map(o => o.urls -> o.updatedAt)
      .update(Some(imageUrls), UtcTime.now)
      .map(_ > 0)
      .pipe(run)

  def queryBulkUpsertAndDeleteTheRestByObjectIdsAndImageType(
      updates: Seq[Update],
      objectIds: Seq[UUID],
      imageType: ImageUploadType,
    ) =
    for {
      us <- queryBulkUpsert(updates)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, t => t.objectId.inSet(objectIds) && t.objectType === imageType)
    } yield records

  def findByObjectIds(objectIds: Seq[UUID], imageUploadType: ImageUploadType): Future[Seq[Record]] =
    if (objectIds.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(t => (t.objectId inSet objectIds) && t.objectType === imageUploadType)
        .result
        .pipe(run)

  def findByObjectIds(objectIds: Seq[UUID]): Future[Seq[Record]] =
    if (objectIds.isEmpty)
      Future.successful(Seq.empty)
    else
      table
        .filter(_.objectId inSet objectIds)
        .result
        .pipe(run)

  def updateObjectId(
      ids: Seq[UUID],
      objectId: UUID,
      merchantId: UUID,
    ): Future[Boolean] =
    if (ids.isEmpty)
      Future.successful(true)
    else
      table
        .withFilter(o => o.id.inSet(ids) && o.merchantId === merchantId)
        .map(o => o.objectId -> o.updatedAt)
        .update(Some(objectId), UtcTime.now)
        .map(_ > 0)
        .pipe(run)
}
