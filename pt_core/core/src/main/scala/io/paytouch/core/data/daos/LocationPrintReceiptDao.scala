package io.paytouch.core.data.daos

import io.paytouch.core.data.daos.features.SlickOneToOneToLocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.LocationPrintReceiptUpsertion
import io.paytouch.core.data.model.{ LocationPrintReceiptRecord, LocationPrintReceiptUpdate }
import io.paytouch.core.data.tables.LocationPrintReceiptsTable

import scala.concurrent.ExecutionContext

class LocationPrintReceiptDao(
    val imageUploadDao: ImageUploadDao,
    val locationDao: LocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickOneToOneToLocationDao {

  type Record = LocationPrintReceiptRecord
  type Update = LocationPrintReceiptUpdate
  type Table = LocationPrintReceiptsTable

  val table = TableQuery[Table]

  def queryUpsert(upsertion: LocationPrintReceiptUpsertion) =
    for {
      (resultType, receiptRecord) <- super.queryUpsert(upsertion.printReceiptUpdate)
      imageUploadUpdates <- asOption(upsertion.imageUploadUpdates.map { img =>
        val imgType = ImageUploadType.PrintReceipt
        val locationIds = Seq(receiptRecord.locationId)
        imageUploadDao.queryBulkUpsertAndDeleteTheRestByObjectIdsAndImageType(img, locationIds, imgType)
      })
    } yield (resultType, receiptRecord)
}
