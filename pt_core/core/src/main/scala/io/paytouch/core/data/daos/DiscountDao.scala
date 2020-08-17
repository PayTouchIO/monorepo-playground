package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao, SlickUpsertDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ DiscountRecord, DiscountUpdate }
import io.paytouch.core.data.model.upsertions.DiscountUpsertion
import io.paytouch.core.data.tables.DiscountsTable
import io.paytouch.core.filters.DiscountFilters
import io.paytouch.core.utils.ResultType

class DiscountDao(
    val discountAvailabilityDao: DiscountAvailabilityDao,
    discountLocationDao: => DiscountLocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao
       with SlickUpsertDao {

  type Record = DiscountRecord
  type Update = DiscountUpdate
  type Upsertion = DiscountUpsertion
  type Filters = DiscountFilters
  type Table = DiscountsTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByMerchantId(merchantId, f.locationIds, f.query, f.updatedSince)(offset, limit)

  def findAllByMerchantId(
      merchantId: UUID,
      locationIds: Option[Seq[UUID]],
      query: Option[String],
      updatedSince: Option[ZonedDateTime],
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    queryFindAllByMerchantId(merchantId, locationIds, query, updatedSince)
      .sortBy(_.title.asc)
      .drop(offset)
      .take(limit)
      .result
      .pipe(run)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByMerchantId(merchantId, f.locationIds, f.query, f.updatedSince)

  def countAllByMerchantId(
      merchantId: UUID,
      locationIds: Option[Seq[UUID]],
      query: Option[String],
      updatedSince: Option[ZonedDateTime],
    ): Future[Int] =
    queryFindAllByMerchantId(merchantId, locationIds, query, updatedSince)
      .length
      .result
      .pipe(run)

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Option[Seq[UUID]],
      query: Option[String],
      updatedSince: Option[ZonedDateTime],
    ) =
    table.filter { t =>
      all(
        Some(t.merchantId === merchantId),
        locationIds.map(lIds => t.id in discountLocationDao.queryFindByLocationIds(lIds).map(_.discountId)),
        query.map(q => t.title.toLowerCase like s"%${q.toLowerCase}%"),
        updatedSince.map { date =>
          any(
            t.id in queryUpdatedSince(date).map(_.id),
            t.id in discountLocationDao.queryUpdatedSince(date).map(_.discountId),
          )
        },
      )
    }

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val upserts = for {
      (resultType, discount) <- queryUpsert(upsertion.discount)
      discountLocations <-
        discountLocationDao
          .queryBulkUpsertAndDeleteTheRest(upsertion.discountLocations, discount.id)
      availabilities <- asOption(
        upsertion
          .availabilities
          .map(availabilities =>
            discountAvailabilityDao.queryBulkUpsertAndDeleteTheRestByItemIds(availabilities, Seq(discount.id)),
          ),
      )
    } yield (resultType, discount)
    runWithTransaction(upserts)
  }

  override def queryDeleteByIdsAndMerchantId(ids: Seq[UUID], merchantId: UUID) =
    for {
      discounts <- super.queryDeleteByIdsAndMerchantId(ids, merchantId)
      discountAvailabilities <- discountAvailabilityDao.queryDeleteByItemIds(ids)
    } yield discounts

}
