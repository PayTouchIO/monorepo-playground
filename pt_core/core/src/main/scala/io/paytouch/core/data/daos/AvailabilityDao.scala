package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.AvailabilityItemType
import io.paytouch.core.data.model.{ AvailabilityRecord, AvailabilityUpdate }
import io.paytouch.core.data.tables.AvailabilitiesTable

import scala.concurrent._

abstract class AvailabilityDao extends SlickMerchantDao {
  final override type Record = AvailabilityRecord
  final override type Update = AvailabilityUpdate
  final override type Table = AvailabilitiesTable

  val table = TableQuery[Table]

  val baseTable = table.filter(_.itemType === itemType)

  def itemType: AvailabilityItemType

  def findByItemId(itemId: UUID): Future[Seq[Record]] =
    findByItemIds(Seq(itemId))

  def findByItemIds(itemIds: Seq[UUID]): Future[Seq[Record]] =
    if (itemIds.isEmpty)
      Future.successful(Seq.empty)
    else
      run(queryByItemIds(itemIds).result)

  def queryDeleteByItemIds(itemIds: Seq[UUID]) =
    queryByItemIds(itemIds).delete

  def queryBulkUpsertAndDeleteTheRestByItemIds(upsertions: Seq[Update], itemIds: Seq[UUID]) =
    for {
      _ <- queryDeleteByItemIds(itemIds)
      us <- queryBulkUpsert(upsertions)
    } yield us

  def queryByItemIds(itemIds: Seq[UUID]) =
    baseTable.filter(_.itemId inSet itemIds)
}
