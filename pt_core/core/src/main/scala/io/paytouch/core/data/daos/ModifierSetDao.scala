package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao, SlickUpsertDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ModifierSetRecord, ModifierSetUpdate }
import io.paytouch.core.data.model.upsertions.ModifierSetUpsertion
import io.paytouch.core.data.tables.ModifierSetsTable
import io.paytouch.core.filters.ModifierSetFilters
import io.paytouch.core.utils.ResultType

class ModifierSetDao(
    modifierSetLocationDao: => ModifierSetLocationDao,
    val modifierOptionDao: ModifierOptionDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao
       with SlickUpsertDao {
  type Record = ModifierSetRecord
  type Update = ModifierSetUpdate
  type Upsertion = ModifierSetUpsertion
  type Filters = ModifierSetFilters
  type Table = ModifierSetsTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int) =
    findAllByMerchantId(merchantId, f.ids, f.locationIds, f.query, f.updatedSince)(offset, limit)

  def countAllWithFilters(merchantId: UUID, f: Filters) =
    countAllByMerchantId(merchantId, f.ids, f.locationIds, f.query, f.updatedSince)

  def findAllByMerchantId(
      merchantId: UUID,
      ids: Option[Seq[UUID]] = None,
      locationIds: Option[Seq[UUID]] = None,
      query: Option[String] = None,
      updatedSince: Option[ZonedDateTime],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    queryFindAllByMerchantId(merchantId, ids, locationIds, query, updatedSince)
      .sortBy(_.name.asc)
      .drop(offset)
      .take(limit)
      .result
      .pipe(run)

  def countAllByMerchantId(
      merchantId: UUID,
      ids: Option[Seq[UUID]] = None,
      locationIds: Option[Seq[UUID]] = None,
      query: Option[String] = None,
      updatedSince: Option[ZonedDateTime],
    ): Future[Int] =
    queryFindAllByMerchantId(merchantId, ids, locationIds, query, updatedSince)
      .length
      .result
      .pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      ids: Option[Seq[UUID]] = None,
      locationIds: Option[Seq[UUID]] = None,
      query: Option[String] = None,
      updatedSince: Option[ZonedDateTime],
    ) =
    table.filter { t =>
      all(
        Some(t.merchantId === merchantId),
        ids.map(_ids => t.id inSet _ids),
        locationIds.map(lIds => t.id in modifierSetLocationDao.queryFindByLocationIds(lIds).map(_.modifierSetId)),
        query.map(q => t.name.toLowerCase like s"%${q.toLowerCase}%"),
        updatedSince.map { date =>
          any(
            t.id in queryUpdatedSince(date).map(_.id),
            t.id in modifierSetLocationDao.queryUpdatedSince(date).map(_.modifierSetId),
          )
        },
      )
    }

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val upserts = for {
      (resultType, modifierSet) <- queryUpsert(upsertion.modifierSet)
      modifierSetLocations <-
        modifierSetLocationDao
          .queryBulkUpsertAndDeleteTheRest(upsertion.modifierSetLocations, modifierSet.id)
      modifierOptions <- asOption(
        upsertion.options.map(modifierOptionDao.queryBulkUpsertAndDeleteTheRestByModifierSetId(_, modifierSet.id)),
      )
    } yield (resultType, modifierSet)
    runWithTransaction(upserts)
  }
}
