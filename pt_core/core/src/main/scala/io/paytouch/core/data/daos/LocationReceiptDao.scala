package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickOneToOneToLocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.LocationReceiptUpsertion
import io.paytouch.core.data.model.{ LocationReceiptRecord, LocationReceiptUpdate }
import io.paytouch.core.data.tables.LocationReceiptsTable

import scala.concurrent._

class LocationReceiptDao(
    val imageUploadDao: ImageUploadDao,
    val locationDao: LocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickOneToOneToLocationDao {

  type Record = LocationReceiptRecord
  type Update = LocationReceiptUpdate
  type Table = LocationReceiptsTable

  val table = TableQuery[Table]

  def queryUpsert(upsertion: LocationReceiptUpsertion) =
    for {
      (resultType, receiptRecord) <- super.queryUpsert(upsertion.receiptUpdate)
      locationIds = Seq(receiptRecord.locationId)
      _ <- asOption(upsertion.emailImageUploadUpdates.map { img =>
        val imgType = ImageUploadType.EmailReceipt
        imageUploadDao.queryBulkUpsertAndDeleteTheRestByObjectIdsAndImageType(img, locationIds, imgType)
      })
      _ <- asOption(upsertion.printImageUploadUpdates.map { img =>
        val imgType = ImageUploadType.PrintReceipt
        imageUploadDao.queryBulkUpsertAndDeleteTheRestByObjectIdsAndImageType(img, locationIds, imgType)
      })
    } yield (resultType, receiptRecord)

  def findByMerchantId(merchantId: UUID): Future[Seq[Record]] = {
    val q = table.filter(_.merchantId === merchantId).sortBy(_.createdAt)
    run(q.result)
  }
}
