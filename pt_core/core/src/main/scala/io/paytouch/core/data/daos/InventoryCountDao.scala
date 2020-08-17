package io.paytouch.core.data.daos

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.{
  SlickFindAllDao,
  SlickLocationTimeZoneHelper,
  SlickMerchantDao,
  SlickUpsertDao,
}
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.InventoryCountStatus
import io.paytouch.core.data.model.upsertions.InventoryCountUpsertion
import io.paytouch.core.data.model.{ InventoryCountRecord, InventoryCountUpdate }
import io.paytouch.core.data.tables.InventoryCountsTable
import io.paytouch.core.filters.InventoryCountFilters
import io.paytouch.core.utils.ResultType

import scala.concurrent._

class InventoryCountDao(
    val inventoryCountProductDao: InventoryCountProductDao,
    val locationDao: LocationDao,
    val nextNumberDao: NextNumberDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao
       with SlickUpsertDao
       with SlickLocationTimeZoneHelper {

  type Record = InventoryCountRecord
  type Update = InventoryCountUpdate
  type Upsertion = InventoryCountUpsertion
  type Filters = InventoryCountFilters
  type Table = InventoryCountsTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId, f.locationId, f.from, f.to, f.query, f.status)(offset, limit)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationId, f.from, f.to, f.query, f.status)

  def findAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      status: Option[InventoryCountStatus],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] = {
    val q =
      queryFindAllByMerchantId(merchantId, locationId, from, to, query, status).drop(offset).take(limit)
    run(q.result)
  }

  def countAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      status: Option[InventoryCountStatus],
    ): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, locationId, from, to, query, status).length.result)

  private def queryFindAllByMerchantId(
      merchantId: UUID,
      locationId: Option[UUID],
      from: Option[LocalDateTime],
      to: Option[LocalDateTime],
      query: Option[String],
      status: Option[InventoryCountStatus],
    ) =
    table
      .filter(t =>
        all(
          Some(t.merchantId === merchantId),
          locationId.map(lId => t.locationId === lId),
          from.map(start => t.id in itemIdsAtOrAfterCreatedAtDate(start)),
          to.map(end => t.id in itemIdsBeforeCreatedAtDate(end)),
          query.map(q => t.number.toLowerCase like s"${q.toLowerCase}%"),
          status.map(s => t.status === s),
        ),
      )
      .sortBy(_.number)

  override def queryInsert(entity: Record) =
    entity match {
      case e if e.number.isEmpty => queryInsertWithIncrementedNumber(e)
      case _                     => super.queryInsert(entity)
    }

  private def queryInsertWithIncrementedNumber(entity: Record) =
    for {
      inventoryCountNumber <- nextNumberDao.queryNextInventoryCountNumberForLocationId(entity.locationId)
      entityWithInventoryCountNumber = entity.copy(number = inventoryCountNumber.toString)
      insert <- table returning table += entityWithInventoryCountNumber
    } yield insert

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val q = for {
      (resultType, upsertedInventoryCount) <- queryUpsert(upsertion.inventoryCount)
      _ <- asOption(
        upsertion
          .inventoryCountProducts
          .map(
            inventoryCountProductDao.queryBulkUpsertAndDeleteTheRestByInventoryCountId(_, upsertedInventoryCount.id),
          ),
      )

      valueChangeAmount <- inventoryCountProductDao.querySumValueChangeAmountByInventoryCountId(
        upsertedInventoryCount.id,
      )
      updatedInventoryCount = upsertedInventoryCount.copy(
        valueChangeAmount = valueChangeAmount,
      )
      inventoryCount <-
        table
          .insertOrUpdate(updatedInventoryCount)
          .map(_ => updatedInventoryCount)

    } yield (resultType, inventoryCount)

    runWithTransaction(q)
  }

  def updateStatus(id: UUID, inventoryCountStatus: InventoryCountStatus): Future[(ResultType, Record)] = {
    val update = InventoryCountUpdate(
      id = Some(id),
      merchantId = None,
      userId = None,
      locationId = None,
      valueChangeAmount = None,
      status = Some(inventoryCountStatus),
      synced = None,
    )
    upsert(update)
  }

  def markAsSyncedAndUpdateStatus(id: UUID, status: InventoryCountStatus): Future[(ResultType, Record)] = {
    val update = InventoryCountUpdate(
      id = Some(id),
      merchantId = None,
      userId = None,
      locationId = None,
      valueChangeAmount = None,
      status = Some(status),
      synced = Some(true),
    )
    upsert(update)
  }
}
