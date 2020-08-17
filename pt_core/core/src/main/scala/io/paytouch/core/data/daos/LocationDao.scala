package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.scalaland.chimney.dsl._

import io.paytouch.implicits._

import io.paytouch.core.data._
import io.paytouch.core.data.daos.features._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.upsertions.LocationUpsertion
import io.paytouch.core.data.tables.LocationsTable
import io.paytouch.core.entities._
import io.paytouch.core.filters.LocationFilters
import io.paytouch.core.utils.ResultType

class LocationDao(
    categoryLocationDao: => CategoryLocationDao,
    locationAvailabilityDao: => LocationAvailabilityDao,
    locationEmailReceiptDao: => LocationEmailReceiptDao,
    locationPrintReceiptDao: => LocationPrintReceiptDao,
    locationReceiptDao: => LocationReceiptDao,
    locationSettingsDao: => LocationSettingsDao,
    nextNumberDao: => NextNumberDao,
    modifierSetLocationDao: => ModifierSetLocationDao,
    productLocationDao: => ProductLocationDao,
    userLocationDao: => UserLocationDao,
    taxRateLocationDao: => TaxRateLocationDao,
    supplierLocationDao: => SupplierLocationDao,
    stockDao: => StockDao,
    loyaltyProgramLocationDao: => LoyaltyProgramLocationDao,
    discountLocationDao: => DiscountLocationDao,
    kitchenDao: => KitchenDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickFindAllDao
       with SlickUpsertDao
       with SlickSoftDeleteDao {
  type Record = model.LocationRecord
  type Update = model.LocationUpdate
  type Upsertion = LocationUpsertion
  type Table = LocationsTable
  type Filters = LocationFilters

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, filters: Filters)(offset: Int, limit: Int) =
    queryAllByMerchantId(merchantId, filters.locationIds, filters.query, filters.showAll)
      .sortBy(_.name.asc)
      .drop(offset)
      .take(limit)
      .result
      .pipe(run)

  def countAllWithFilters(merchantId: UUID, filters: Filters) =
    queryAllByMerchantId(merchantId, filters.locationIds, filters.query, filters.showAll)
      .length
      .result
      .pipe(run)

  def queryAllByMerchantId(
      merchantId: UUID,
      locationIds: Seq[UUID],
      query: Option[String],
      showAll: Option[Boolean],
    ) = {
    val filterByLocations = showAll.orElse(Some(false)).filter(x => !x)
    queryFindAllByMerchantId(merchantId).filter(t =>
      all(
        filterByLocations.map(_ => t.id inSet locationIds),
        query.map(q => t.name.toLowerCase like s"%${q.toLowerCase}%"),
      ),
    )
  }

  def findByProductId(productId: UUID): Future[Seq[Record]] =
    nonDeletedTable
      .filter(_.id in productLocationDao.queryFindByItemId(productId).map(_.locationId))
      .result
      .pipe(run)

  def findAllByModifierSetId(modifierSetId: UUID): Future[Seq[Record]] =
    nonDeletedTable
      .filter(_.id in modifierSetLocationDao.queryFindByItemId(modifierSetId).map(_.locationId))
      .result
      .pipe(run)

  def findAllByCategoryId(categoryId: UUID) =
    nonDeletedTable
      .filter(_.id in categoryLocationDao.queryFindByItemId(categoryId).map(_.locationId))
      .result
      .pipe(run)

  def findAllByUserIds(userIds: Seq[UUID]): Future[Map[UUID, Seq[Record]]] =
    if (userIds.isEmpty)
      Future.successful(Map.empty)
    else
      nonDeletedTable
        .join(userLocationDao.queryFindByItemIds(userIds))
        .on(_.id === _.locationId)
        .map {
          case (locationsTable, userLocationsTable) =>
            userLocationsTable.userId -> locationsTable
        }
        .result
        .pipe(run)
        .map(_.groupBy {
          case (userId, _) =>
            userId
        }.transform((_, v) => v.map { case (_, location) => location }))

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] =
    (for {
      (resultType, location) <- queryUpsert(upsertion.location)
      _ <- asOption(upsertion.locationSettings.map(locationSettingsDao.queryUpsert))
      _ <- locationEmailReceiptDao.queryUpsert(upsertion.locationEmailReceiptUpdate)
      _ <- locationPrintReceiptDao.queryUpsert(upsertion.locationPrintReceiptUpdate)
      _ <- locationReceiptDao.queryUpsert(upsertion.locationReceiptUpdate)
      _ <- locationAvailabilityDao.queryBulkUpsertAndDeleteTheRestByItemIds(upsertion.availabilities, Seq(location.id))
      _ <- userLocationDao.queryBulkUpsertByRelIds(upsertion.userLocations)
      _ <- asOption(upsertion.initialOrderNumber.map { numb =>
        nextNumberDao.queryInsertNewNextNumber(
          scope = Scope.fromLocationId(location.id),
          `type` = NextNumberType.Order,
          startNumb = numb,
        )
      })
    } yield (resultType, location)).pipe(runWithTransaction)

  def findFirstByMerchantId(merchantId: UUID): Future[Option[Record]] =
    queryFindAllByMerchantId(merchantId)
      .sortBy(_.createdAt)
      .result
      .headOption
      .pipe(run)

  def deepCopy(from: UUID, to: UUID): Future[Unit] =
    deepCopyQuery(from, to).pipe(runWithTransaction)

  def deepCopyQuery(from: UUID, to: UUID) =
    for {
      _ <-
        userLocationDao
          .queryFindByLocationId(to)
          .result
          .flatMap { targetUserLocations =>
            userLocationDao.queryFindByLocationId(from).result.flatMap {
              _.filterNot { sourceUserLocation =>
                targetUserLocations.exists(_.userId === sourceUserLocation.userId)
              }.map {
                _.into[model.UserLocationUpdate]
                  .withFieldConst(_.id, UUID.randomUUID.some)
                  .withFieldConst(_.locationId, to.some)
                  .transform
              }.pipe(userLocationDao.queryBulkUpsert)
            }
          }
      _ <- taxRateLocationDao.queryFindByLocationId(from).result.flatMap {
        _.map {
          _.into[model.TaxRateLocationUpdate]
            .withFieldConst(_.id, UUID.randomUUID.some)
            .withFieldConst(_.locationId, to.some)
            .transform
        }.pipe(taxRateLocationDao.queryBulkUpsert)
      }
      _ <- supplierLocationDao.queryFindByLocationId(from).result.flatMap {
        _.map {
          _.into[model.SupplierLocationUpdate]
            .withFieldConst(_.id, UUID.randomUUID.some)
            .withFieldConst(_.locationId, to.some)
            .transform
        }.pipe(supplierLocationDao.queryBulkUpsert)
      }
      _ <- stockDao.queryFindByLocationId(from).result.flatMap {
        _.map {
          _.into[model.StockUpdate]
            .withFieldConst(_.id, UUID.randomUUID.some)
            .withFieldConst(_.locationId, to.some)
            .transform
        }.pipe(stockDao.queryBulkUpsert)
      }
      _ <- modifierSetLocationDao.queryFindByLocationId(from).result.flatMap {
        _.map {
          _.into[model.ModifierSetLocationUpdate]
            .withFieldConst(_.id, UUID.randomUUID.some)
            .withFieldConst(_.locationId, to.some)
            .transform
        }.pipe(modifierSetLocationDao.queryBulkUpsert)
      }
      _ <- loyaltyProgramLocationDao.queryFindByLocationId(from).result.flatMap {
        _.map {
          _.into[model.LoyaltyProgramLocationUpdate]
            .withFieldConst(_.id, UUID.randomUUID.some)
            .withFieldConst(_.locationId, to.some)
            .transform
        }.pipe(loyaltyProgramLocationDao.queryBulkUpsert)
      }
      _ <- discountLocationDao.queryFindByLocationId(from).result.flatMap {
        _.map {
          _.into[model.DiscountLocationUpdate]
            .withFieldConst(_.id, UUID.randomUUID.some)
            .withFieldConst(_.locationId, to.some)
            .transform
        }.pipe(discountLocationDao.queryBulkUpsert)
      }
      _ <- categoryLocationDao.queryFindByLocationId(from).result.flatMap {
        _.map {
          _.into[model.CategoryLocationUpdate]
            .withFieldConst(_.id, UUID.randomUUID.some)
            .withFieldConst(_.locationId, to.some)
            .transform
        }.pipe(categoryLocationDao.queryBulkUpsert)
      }
      _ <- kitchenDao.queryFindByLocationId(from).result.flatMap { kitchens =>
        val (kitchensQuery, oldKitchenIdToNewKitchenId) =
          kitchens
            .foldLeft(Vector.empty[model.KitchenUpdate], Map.empty[UUID, UUID]) {
              case ((kitchenUpdates, lookup), current) =>
                val newKitchenId =
                  UUID.randomUUID

                val kitchenUpdate =
                  current
                    .into[model.KitchenUpdate]
                    .withFieldConst(_.id, newKitchenId.some)
                    .withFieldConst(_.locationId, to.some)
                    .transform

                (kitchenUpdates :+ kitchenUpdate) -> (lookup + (current.id -> newKitchenId))
            }
            .bimap(kitchenDao.queryBulkUpsert, _.get _)

        val productLocationsQuery =
          productLocationDao.queryFindByLocationId(from).result.flatMap {
            _.map {
              _.into[model.ProductLocationUpdate]
                .withFieldConst(_.id, UUID.randomUUID.some)
                .withFieldConst(_.locationId, to.some)
                .withFieldComputed(_.routeToKitchenId, _.routeToKitchenId.flatMap(oldKitchenIdToNewKitchenId))
                .transform
            }.pipe(productLocationDao.queryBulkUpsert)
          }

        DBIO.seq(kitchensQuery, productLocationsQuery)
      }
    } yield ()
}
