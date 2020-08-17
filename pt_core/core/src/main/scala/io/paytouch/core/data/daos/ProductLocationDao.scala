package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.implicits._

import scala.concurrent._

import slick.jdbc.GetResult

import io.paytouch.core.data.daos.features.SlickItemLocationToggleableDao
import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ProductLocationRecord, ProductLocationUpdate }
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.data.tables.ProductLocationsTable
import io.paytouch.core.utils.UtcTime

class ProductLocationDao(
    articleDao: => ArticleDao,
    val locationDao: LocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickItemLocationToggleableDao {
  type Record = ProductLocationRecord
  type Update = ProductLocationUpdate
  type Table = ProductLocationsTable

  val table = TableQuery[Table]

  val itemDao = articleDao

  def queryFindAllByProductIdWithVariants(productId: UUID, locationId: Option[UUID]) =
    table.filter(t =>
      all(
        Some(t.productId in articleDao.queryFindByIdWithVariants(productId).map(_.id)),
        locationId.map(lId => t.locationId === lId),
      ),
    )

  def queryFindByOptionalLocationId(locationId: Option[UUID]) =
    table.filter(t =>
      all(
        locationId.map(lId => t.locationId === lId),
      ),
    )

  def queryFindByProductsAndLocationFilter(productIds: Seq[UUID], locationId: Option[UUID]) =
    table.filter(t =>
      all(
        Some(t.productId inSet productIds),
        locationId.map(lId => t.locationId === lId),
      ),
    )

  def queryByRelIds(productLocationUpdate: Update) = {
    require(
      productLocationUpdate.productId.isDefined,
      "ProductLocationDao - Impossible to find by product id and location id without a product id",
    )

    require(
      productLocationUpdate.locationId.isDefined,
      "ProductLocationDao - Impossible to find by product id and location id without a location id",
    )

    queryFindByItemIdAndLocationId(productLocationUpdate.productId.get, productLocationUpdate.locationId.get)
  }

  def queryBulkUpsertAndDeleteTheRestByProductIds(productLocations: Seq[Update], productIds: Seq[UUID]) =
    queryBulkUpsertAndDeleteTheRestByRelIds(productLocations, t => t.productId inSet productIds)

  def findByProductIdAndLocationId(productId: UUID, locationId: UUID): Future[Option[Record]] =
    findByProductIdsAndLocationIds(Seq(productId), Seq(locationId)).map(_.headOption)

  def findByProductIdsAndLocationId(productIds: Seq[UUID], locationId: UUID): Future[Seq[Record]] =
    if (productIds.isEmpty)
      Future.successful(Seq.empty)
    else
      findByProductIdsAndLocationIds(productIds, Seq(locationId))

  def findByProductIdsAndLocationIds(productIds: Seq[UUID], locationIds: Seq[UUID]): Future[Seq[Record]] =
    if (productIds.isEmpty || locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      findByItemIdsAndLocationIds(productIds, locationIds)

  def updateAverageCosts(productAverageCostsByLocation: Map[UUID, BigDecimal], locationId: UUID) =
    productAverageCostsByLocation
      .map {
        case (productId, averageCost) => queryUpdateAverageCost(productId, locationId, averageCost)
      }
      .pipe(asTraversable)
      .pipe(runWithTransaction)

  private def queryUpdateAverageCost(
      productId: UUID,
      locationId: UUID,
      averageCost: BigDecimal,
    ) =
    table
      .withFilter(o => o.productId === productId && o.locationId === locationId)
      .map(o => o.averageCostAmount -> o.updatedAt)
      .update(Some(averageCost), UtcTime.now)

  private def findMonetaryRangeByProductIds(
      columnName: String,
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    ): Future[Map[UUID, (BigDecimal, BigDecimal)]] =
    if (productIds.isEmpty || locationIds.isEmpty)
      Future.successful(Map.empty)
    else {
      implicit val getMapResult = GetResult(r => (r.nextUUID(), (r.nextBigDecimal(), r.nextBigDecimal())))

      sql"""
             WITH main AS (
                 SELECT p.id, p.type
                 FROM products p
                 WHERE p.id IN (#${productIds.asInParametersList})
                   AND p.deleted_at IS NULL
               ),
               storable AS (
                 SELECT p.id, p.is_variant_of_product_id AS parent_id, p.type
                 FROM products p
                 WHERE p.is_variant_of_product_id IN (#${productIds.asInParametersList})
                   AND p.deleted_at IS NULL
                   AND p.type IN (#${ArticleType.storables.map(_.entryName).asInParametersList}))

             SELECT main.id,
                COALESCE(MIN(pl.#$columnName), 0) AS minimum,
                COALESCE(MAX(pl.#$columnName), 0) AS maximum
             FROM main
             JOIN storable
             ON main.id = storable.parent_id
             LEFT JOIN product_locations pl
             ON storable.id = pl.product_id
             WHERE pl.location_id IN (#${locationIds.asInParametersList})
             GROUP BY main.id
            ;"""
        .as[(UUID, (BigDecimal, BigDecimal))]
        .pipe(run)
        .map(
          _.groupBy { case (productId, _) => productId }
            .transform { (_, v) =>
              v.map { case (_, priceR) => priceR }.head
            },
        )
    }

  def findPriceRangesByProductIds(
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    ): Future[Map[UUID, (BigDecimal, BigDecimal)]] =
    findMonetaryRangeByProductIds("price_amount", productIds, locationIds)

  def findCostRangesByProductIds(
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    ): Future[Map[UUID, (BigDecimal, BigDecimal)]] =
    findMonetaryRangeByProductIds("cost_amount", productIds, locationIds)

  def findByRoutingToKitchenId(kitchenIds: Seq[UUID]) =
    table
      .filter(_.routeToKitchenId inSet kitchenIds)
      .result
      .pipe(run)
}
