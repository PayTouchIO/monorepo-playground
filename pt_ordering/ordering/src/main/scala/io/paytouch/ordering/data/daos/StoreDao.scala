package io.paytouch.ordering.data.daos

import java.util.UUID

import scala.concurrent._

import slick.lifted.{ CanBeQueryCondition, Rep }

import io.paytouch.ordering.data.daos.features._
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.model.{ StoreRecord, StoreUpdate }
import io.paytouch.ordering.data.tables.StoresTable
import io.paytouch.ordering.entities.StoreContext
import io.paytouch.ordering.filters.NoFilters

class StoreDao(
    val cartDao: CartDao,
    val merchantDao: MerchantDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickLocationDao
       with SlickFindAllDao
       with SlickUpsertDao
       with SlickToggleableItemDao {
  type Filters = NoFilters
  type Record = StoreRecord
  type Table = StoresTable
  type Update = StoreUpdate
  type Upsertion = Update

  val table = TableQuery[Table]

  def findAllWithFilters(locationIds: Seq[UUID], filters: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByLocationIds(locationIds)(offset, limit)

  def countAllWithFilters(locationIds: Seq[UUID], filters: Filters): Future[Int] =
    countAllByLocationIds(locationIds)

  def findStoreContextById(id: UUID): Future[Option[StoreContext]] =
    findStoreContextByFilter(_.id === id)

  def findStoreContextByCartId(cartId: UUID): Future[Option[StoreContext]] =
    findStoreContextByFilter(_.id in cartDao.queryById(cartId).map(_.storeId))

  def findStoreContextByLocationId(locationId: UUID): Future[Option[StoreContext]] =
    findStoreContextByFilter(_.locationId === locationId)

  def findStoreContextByCatalogId(catalogId: UUID): Future[Option[StoreContext]] =
    findStoreContextByFilter(_.catalogId === catalogId)

  private def findStoreContextByFilter[T <: Rep[_]](
      predicate: Table => T,
    )(implicit
      wt: CanBeQueryCondition[T],
    ): Future[Option[StoreContext]] = {
    val q = table.filter(predicate).map(_.storeContext)

    run(q.result.headOption)
  }

  def existsLocationId(idToExclude: UUID, locationId: UUID): Future[Boolean] = {
    val q = queryByNotId(idToExclude).filter(_.locationId === locationId).exists

    run(q.result)
  }

  def existsMerchantIdAndUrlSlug(
      idToExclude: UUID,
      merchantId: UUID,
      urlSlug: String,
    ): Future[Boolean] = {
    val q = queryByNotId(idToExclude).filter(_.merchantId === merchantId).filter(_.urlSlug === urlSlug).exists

    run(q.result)
  }

  def findAllByCatalogIds(catalogIds: Seq[UUID]): Future[Seq[Record]] = {
    if (catalogIds.isEmpty) return Future.successful(Seq.empty)

    val q = table.filter(_.catalogId inSet catalogIds)

    run(q.result)
  }

  def findByMerchantSlugAndSlug(merchantSlug: String, slug: String): Future[Option[Record]] = {
    val q = table
      .filter(_.urlSlug === slug)
      .filter(_.merchantId in merchantDao.queryFindBySlug(merchantSlug).map(_.id))

    run(q.result.headOption)
  }

  def findByMerchantId(merchantId: UUID): Future[Seq[Record]] = {
    val q = table.filter(_.merchantId === merchantId)

    run(q.result)
  }
}
