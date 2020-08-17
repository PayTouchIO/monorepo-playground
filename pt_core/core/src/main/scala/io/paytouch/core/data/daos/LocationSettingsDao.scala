package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ImageUploadType
import io.paytouch.core.data.model.upsertions.LocationSettingsUpsertion
import io.paytouch.core.data.tables.LocationSettingsTable
import io.paytouch.core.entities.UpdateActiveItem
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.UtcTime

class LocationSettingsDao(
    val locationEmailReceiptDao: LocationEmailReceiptDao,
    val locationPrintReceiptDao: LocationPrintReceiptDao,
    val locationReceiptDao: LocationReceiptDao,
    val locationDao: LocationDao,
    val imageUploadDao: ImageUploadDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickOneToOneToLocationDao
       with SlickUpsertDao {
  type Record = LocationSettingsRecord
  type Update = LocationSettingsUpdate
  type Upsertion = LocationSettingsUpsertion
  type Table = LocationSettingsTable

  val table = TableQuery[Table]

  final override def upsert(upsertion: Upsertion): Future[(ResultType, Record)] =
    (for {
      r @ (resultType, locationSettings) <- queryUpsert(upsertion.locationSettings)
      _ <- locationDao.queryMarkAsUpdatedById(locationSettings.locationId)
      _ <- asOption(upsertion.emailReceiptUpdate.map(locationEmailReceiptDao.queryUpsert))
      _ <- asOption(upsertion.printReceiptUpdate.map(locationPrintReceiptDao.queryUpsert))
      _ <- asOption(upsertion.receiptUpdate.map(locationReceiptDao.queryUpsert))
      _ <- asOption(upsertion.splashImageUploads.map(queryUpsertImagesAndDeleteTheRest(_, locationSettings.locationId)))
    } yield r).pipe(runWithTransaction)

  private def queryUpsertImagesAndDeleteTheRest(imageUploads: Seq[ImageUploadUpdate], objectId: UUID) = {
    val imgType = ImageUploadType.CfdSplashScreen
    imageUploadDao.queryBulkUpsertAndDeleteTheRestByObjectIdsAndImageType(imageUploads, Seq(objectId), imgType)
  }

  def findLatestSettings(merchantId: UUID): Future[Option[Record]] = {
    val q = table.filter(_.merchantId === merchantId).sortBy(_.createdAt.desc)
    run(q.result.headOption)
  }

  def bulkUpdateOnlineStorefrontEnabled(merchantId: UUID, updates: Seq[UpdateActiveItem]): Future[Unit] =
    bulkUpdateBooleanField(merchantId, updates, _.onlineStorefrontEnabled)

  def bulkUpdateRapidoEnabled(merchantId: UUID, updates: Seq[UpdateActiveItem]): Future[Unit] =
    bulkUpdateBooleanField(merchantId, updates, _.rapidoEnabled)

  def setOnlineStorefrontEnabled(merchantId: UUID, locationId: UUID): Future[Int] =
    querySetBooleanField(merchantId, Seq(locationId), true, _.onlineStorefrontEnabled).pipe(runWithTransaction)

  def setDeliveryProvidersEnabled(merchantId: UUID, locationId: UUID): Future[Int] =
    querySetBooleanField(merchantId, Seq(locationId), true, _.deliveryProvidersEnabled).pipe(runWithTransaction)

  private def bulkUpdateBooleanField(
      merchantId: UUID,
      updates: Seq[UpdateActiveItem],
      field: Table => Rep[Boolean],
    ): Future[Unit] = {
    val idsToActivate = updates.filter(_.active).map(_.itemId)
    val idsToDeactivate = updates.filterNot(_.active).map(_.itemId)

    (for {
      _ <- querySetBooleanField(merchantId, idsToActivate, true, field)
      _ <- querySetBooleanField(merchantId, idsToDeactivate, false, field)
    } yield ()).pipe(runWithTransaction)
  }

  private def querySetBooleanField(
      merchantId: UUID,
      locationIds: Seq[UUID],
      value: Boolean,
      field: Table => Rep[Boolean],
    ) =
    table
      .filter(o => o.locationId.inSet(locationIds) && o.merchantId === merchantId)
      .map(o => field(o) -> o.updatedAt)
      .update(value, UtcTime.now)
}
