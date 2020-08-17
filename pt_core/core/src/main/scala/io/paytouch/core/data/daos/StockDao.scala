package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import scala.concurrent._

import slick.jdbc.GetResult

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ StockRecord, StockUpdate }
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.data.tables.StocksTable
import io.paytouch.core.filters.StockFilters

class StockDao(
    val locationDao: LocationDao,
    val articleDao: ArticleDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickMerchantDao
       with SlickFindAllDao {
  type Record = StockRecord
  type Update = StockUpdate
  type Filters = StockFilters
  type Table = StocksTable

  val table = TableQuery[Table]

  private def nonDeletedTable =
    table
      .join(articleDao.nonDeletedTable)
      .on { case (stocksTable, articlesTable) => stocksTable.productId === articlesTable.id }
      .map { case (stocksTable, _) => stocksTable }
      .join(locationDao.nonDeletedTable)
      .on { case (stocksTable, locationsTable) => stocksTable.locationId === locationsTable.id }
      .map { case (stocksTable, _) => stocksTable }

  implicit val l: LocationDao = locationDao

  def findByProductIdsAndMerchantId(productIds: Seq[UUID], merchantId: UUID): Future[Seq[Record]] =
    if (productIds.isEmpty)
      Seq.empty.pure[Future]
    else
      nonDeletedTable
        .filter(t => t.productId.inSet(productIds) && t.merchantId === merchantId)
        .result
        .pipe(run)

  def findByProductIdsAndLocationIds(productIds: Seq[UUID], locationIds: Seq[UUID]): Future[Seq[Record]] =
    if (productIds.isEmpty || locationIds.isEmpty)
      Seq.empty.pure[Future]
    else
      nonDeletedTable
        .filter(t => t.productId.inSet(productIds) && t.locationId.inSet(locationIds))
        .result
        .pipe(run)

  def findByProductIdAndLocationIds(
      merchantId: UUID,
      productId: UUID,
      locationIds: Seq[UUID],
    ) =
    queryFindAllByProductId(
      merchantId = merchantId,
      mainProductId = Some(productId),
      locationIds = Some(locationIds),
      updatedSince = None,
    ).result.headOption.pipe(run)

  def findAllByProductIds(
      merchantId: UUID,
      productIds: Seq[UUID],
      locationIds: Option[Seq[UUID]],
    ): Future[Seq[Record]] =
    if (productIds.isEmpty)
      Seq.empty.pure[Future]
    else
      queryFindAllByProductIds(
        merchantId = merchantId,
        productIds = productIds,
        locationIds = locationIds,
        updatedSince = None,
      ).result.pipe(run)

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    findAllByProductId(
      merchantId = merchantId,
      productId = f.productId,
      locationIds = f.locationIds,
      updatedSince = f.updatedSince,
    )(offset, limit)

  def findAllByProductId(
      merchantId: UUID,
      productId: Option[UUID],
      locationIds: Option[Seq[UUID]] = None,
      updatedSince: Option[ZonedDateTime] = None,
    )(
      offset: Int,
      limit: Int,
    ): Future[Seq[Record]] =
    queryFindAllByProductId(merchantId, productId, locationIds, updatedSince)
      .drop(offset)
      .take(limit)
      .result
      .pipe(run)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    countAllByProductId(
      merchantId = merchantId,
      productId = f.productId,
      locationIds = f.locationIds,
      updatedSince = f.updatedSince,
    )

  def countAllByProductId(
      merchantId: UUID,
      productId: Option[UUID],
      locationIds: Option[Seq[UUID]] = None,
      updatedSince: Option[ZonedDateTime],
    ): Future[Int] =
    queryFindAllByProductId(
      merchantId = merchantId,
      mainProductId = productId,
      locationIds = locationIds,
      updatedSince = updatedSince,
    ).length.result.pipe(run)

  def queryFindAllByProductId(
      merchantId: UUID,
      mainProductId: Option[UUID],
      locationIds: Option[Seq[UUID]],
      updatedSince: Option[ZonedDateTime],
    ) =
    mainProductId match {
      case Some(productId) =>
        queryFindAllByProductIds(
          merchantId = merchantId,
          productIds = Seq(productId),
          locationIds = locationIds,
          updatedSince = updatedSince,
        )

      case None =>
        queryFindAllByMerchantId(
          merchantId = merchantId,
          locationIds = locationIds,
          updatedSince = updatedSince,
        )
    }

  def queryFindAllByProductIds(
      merchantId: UUID,
      productIds: Seq[UUID],
      locationIds: Option[Seq[UUID]],
      updatedSince: Option[ZonedDateTime],
    ) =
    nonDeletedTable
      .filter(_.merchantId === merchantId)
      .filter(t =>
        all(
          locationIds.map(locationIds => t.locationId inSet locationIds),
          updatedSince.map(date => t.id in queryUpdatedSince(date).map(_.id)),
        ),
      )
      .join(articleDao.queryFindByIdsWithVariants(productIds))
      .on { case (stocksTable, productsTable) => stocksTable.productId === productsTable.id }
      .map { case (stocksTable, _) => stocksTable }

  def queryFindAllByMerchantId(
      merchantId: UUID,
      locationIds: Option[Seq[UUID]],
      updatedSince: Option[ZonedDateTime],
    ) =
    nonDeletedTable
      .filter(_.merchantId === merchantId)
      .filter(t =>
        all(
          locationIds.map(lIds => t.locationId inSet lIds),
          updatedSince.map(date => t.id in queryUpdatedSince(date).map(_.id)),
        ),
      )
      .join(
        articleDao
          .queryFindAllByMerchantId(
            merchantId,
            articleTypes = Some(ArticleType.storables),
            locationIds = locationIds,
          ),
      )
      .on { case (stocksTable, productsTable) => stocksTable.productId === productsTable.id }
      .sortBy {
        case (_, productsTable) =>
          (productsTable.name.asc, productsTable.id)
      }
      .map { case (stocksTable, _) => stocksTable }

  def findStockLevelByVariantProductIds(
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    ): Future[Map[UUID, Map[UUID, BigDecimal]]] =
    findFieldByVariantProductIds(productIds = productIds, locationIds = locationIds)(_.quantity)

  def findStockLevelByProductsIds(
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    ): Future[Map[UUID, Map[UUID, BigDecimal]]] =
    findFieldByProductsIds(productIds = productIds, locationIds = locationIds)(_.quantity)

  def queryProductsWithLowInventory(merchantId: UUID, locationIds: Option[Seq[UUID]] = None) =
    queryProductsWithInventoryQuantity(merchantId, locationIds)(_ <= _)

  def queryProductsWithHighInventory(merchantId: UUID, locationIds: Option[Seq[UUID]] = None) =
    queryProductsWithInventoryQuantity(merchantId, locationIds)(_ > _)

  private def queryProductsWithInventoryQuantity(
      merchantId: UUID,
      locationIds: Option[Seq[UUID]],
    )(
      op: (Rep[BigDecimal], Rep[BigDecimal]) => Rep[Boolean],
    ) =
    table
      .join(articleDao.nonDeletedTable)
      .on { case (stocksTable, articlesTable) => stocksTable.productId === articlesTable.id }
      .filter {
        case (t, _) =>
          all(
            Some(t.merchantId === merchantId),
            Some(op(t.quantity, t.minimumOnHand)),
            locationIds.map(lIds => t.locationId inSet lIds),
          )
      }
      .map { case (_, articlesTable) => articlesTable }

  override def queryUpsert(update: Update) = {
    require(
      update.productId.isDefined,
      "StockDao - impossible to upsert by product and location id without a product id",
    )

    require(
      update.locationId.isDefined,
      "StockDao - impossible to upsert by product and location id without a location id",
    )

    queryUpsertByQuery(
      update,
      queryFindOneByProductIdAndLocationId(
        update.productId.get,
        update.locationId.get,
      ),
    )
  }

  private def queryFindOneByProductIdAndLocationId(productId: UUID, locationId: UUID) =
    nonDeletedTable.filter(t => t.productId === productId && t.locationId === locationId)

  def increaseStockQuantity(
      productId: UUID,
      locationId: UUID,
      quantity: BigDecimal,
    ): Future[Unit] = {
    implicit val unitGetResult: GetResult[Unit] =
      GetResult(_ => ())

    sql"""UPDATE stocks
          SET quantity = quantity + #$quantity,
          updated_at = now()
          WHERE product_id = '#$productId'
          AND location_id = '#$locationId';""".as[Unit].pipe(runWithTransaction).void
  }

  def findReorderAmountByProductsIds(
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    ): Future[Map[UUID, Map[UUID, BigDecimal]]] =
    findFieldByProductsIds(productIds, locationIds)(_.reorderAmount)

  private def findFieldByProductsIds(
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    )(
      fieldExtractor: Table => Rep[BigDecimal],
    ): Future[Map[UUID, Map[UUID, BigDecimal]]] =
    for {
      mainFieldAmount <- findFieldByMainProductIds(productIds, locationIds)(fieldExtractor)
      variantsFieldAmount <- findFieldByVariantProductIds(productIds, locationIds)(fieldExtractor)
    } yield mainFieldAmount ++ variantsFieldAmount

  private def findFieldByMainProductIds(
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    )(
      fieldExtractor: Table => Rep[BigDecimal],
    ): Future[Map[UUID, Map[UUID, BigDecimal]]] =
    if (productIds.isEmpty || locationIds.isEmpty)
      Map.empty.pure[Future]
    else
      queryFieldByMainProductIds(productIds, locationIds)(fieldExtractor)
        .result
        .pipe(run)
        .map { rows =>
          rows
            .groupBy(_._1)
            .transform { (_, v1) =>
              v1.groupBy(_._2).transform((_, v2) => v2.flatMap(_._3).sum)
            }
        }

  private def findFieldByVariantProductIds(
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    )(
      fieldExtractor: Table => Rep[BigDecimal],
    ): Future[Map[UUID, Map[UUID, BigDecimal]]] =
    if (productIds.isEmpty || locationIds.isEmpty)
      Map.empty.pure[Future]
    else
      queryFieldByVariantProductIds(productIds, locationIds)(fieldExtractor)
        .result
        .pipe(run)
        .map { rows =>
          rows
            .groupBy(_._1)
            .transform { (_, v1) =>
              v1.groupBy(_._2).transform((_, v2) => v2.map(_._3).sum)
            }
        }

  private def queryFieldByMainProductIds(
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    )(
      fieldExtractor: Table => Rep[BigDecimal],
    ) =
    nonDeletedTable
      .filter(_.locationId inSet locationIds)
      .join(articleDao.queryFindMainByParentIds(productIds))
      .on { case (stocksTable, productsTable) => stocksTable.productId === productsTable.id }
      .map {
        case (stocksTable, productsTable) =>
          (productsTable.isVariantOfProductId, stocksTable.locationId, fieldExtractor(stocksTable))
      }
      .groupBy { case (isVariantOfProductId, locationId, _) => (isVariantOfProductId, locationId) }
      .map {
        case ((productId, locationId), amounts) =>
          (productId.get, locationId, amounts.map { case (_, _, amount) => amount }.sum)
      }

  private def queryFieldByVariantProductIds(
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    )(
      fieldExtractor: Table => Rep[BigDecimal],
    ) =
    nonDeletedTable
      .filter(_.productId inSet productIds)
      // NB: don't use LocationIdQuery.filterByLocationIds to avoid a super slow extra join
      .filter(_.locationId inSet locationIds)
      .map(row => (row.productId, row.locationId, fieldExtractor(row)))

  def findByLocationIds(locationIds: Seq[UUID]): Future[Seq[Record]] =
    if (locationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByLocationIds(locationIds)
        .result
        .pipe(run)

  def findByLocationId(locationId: UUID): Future[Seq[Record]] =
    findByLocationIds(Seq(locationId))

  def queryFindByLocationId(locationId: UUID) =
    queryFindByLocationIds(Seq(locationId))

  def queryFindByLocationIds(locationIds: Seq[UUID]) =
    nonDeletedTable.filterByLocationIds(locationIds)
}
