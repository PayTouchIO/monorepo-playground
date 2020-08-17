package io.paytouch.core.data.daos

import scala.concurrent._

import io.paytouch.core.data.daos.features.SlickOneToOneToLocationDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ LocationEmailReceiptRecord, LocationEmailReceiptUpdate }
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.LocationEmailReceiptUpsertion
import io.paytouch.core.data.tables.LocationEmailReceiptsTable

class LocationEmailReceiptDao(
    val imageUploadDao: ImageUploadDao,
    val locationDao: LocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickOneToOneToLocationDao {

  type Record = LocationEmailReceiptRecord
  type Update = LocationEmailReceiptUpdate
  type Table = LocationEmailReceiptsTable

  val table = TableQuery[Table]

  def queryUpsert(upsertion: LocationEmailReceiptUpsertion) =
    for {
      (resultType, receiptRecord) <- super.queryUpsert(upsertion.emailReceiptUpdate)
      imageUploadUpdates <- asOption(upsertion.imageUploadUpdates.map { img =>
        val imgType = ImageUploadType.EmailReceipt
        val locationIds = Seq(receiptRecord.locationId)
        imageUploadDao.queryBulkUpsertAndDeleteTheRestByObjectIdsAndImageType(img, locationIds, imgType)
      })
    } yield (resultType, receiptRecord)
}
