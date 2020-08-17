package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ProductLocationTaxRateRecord, ProductLocationTaxRateUpdate }
import io.paytouch.core.data.tables.ProductLocationTaxRatesTable

class ProductLocationTaxRateDao(
    productLocationDao: => ProductLocationDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickRelDao {
  type Record = ProductLocationTaxRateRecord
  type Update = ProductLocationTaxRateUpdate
  type Table = ProductLocationTaxRatesTable

  val table = TableQuery[Table]

  def queryFindAllByMerchantId(merchantId: UUID, locationIds: Option[Seq[UUID]] = None) =
    table.filter(t =>
      all(
        Some(t.merchantId === merchantId),
        locationIds.map(lIds => t.productLocationId in productLocationDao.queryFindByLocationIds(lIds).map(_.id)),
      ),
    )

  def queryFindByProductLocationIdAndTaxRateId(productLocationId: UUID, taxRateId: UUID) =
    queryFindByProductLocationIdsAndTaxRateIds(Seq(productLocationId), Seq(taxRateId))

  def queryFindByProductLocationIdsAndTaxRateIds(productLocationIds: Seq[UUID], taxRateIds: Seq[UUID]) =
    table.filter(_.productLocationId inSet productLocationIds).filter(_.taxRateId inSet taxRateIds)

  def queryFindByProductLocationIds(productLocationIds: Seq[UUID]) =
    table.filter(_.productLocationId inSet productLocationIds)

  def queryFindByTaxRateIds(taxRateIds: Seq[UUID]) =
    table.filter(_.taxRateId inSet taxRateIds)

  def queryByRelIds(productLocationTaxRate: Update) = {
    require(
      productLocationTaxRate.productLocationId.isDefined,
      "ProductLocationTaxRateDao - Impossible to find by product location id and tax rate id without a product location id",
    )

    require(
      productLocationTaxRate.taxRateId.isDefined,
      "ProductLocationTaxRateDao - Impossible to find by product location id and tax rate id without a tax rate id",
    )

    queryFindByProductLocationIdAndTaxRateId(
      productLocationTaxRate.productLocationId.get,
      productLocationTaxRate.taxRateId.get,
    )
  }

  def queryBulkUpsertAndDeleteTheRestByProductLocationIds(
      productLocationTaxRates: Seq[Update],
      productLocationIds: Seq[UUID],
    ) =
    queryBulkUpsertAndDeleteTheRestByRelIds(productLocationTaxRates, t => t.productLocationId inSet productLocationIds)

  def bulkUpsertAndDeleteTheRestByProductLocationIds(
      productLocationTaxRates: Seq[Update],
      productLocationIds: Seq[UUID],
    ): Future[Seq[Record]] =
    queryBulkUpsertAndDeleteTheRestByProductLocationIds(productLocationTaxRates, productLocationIds)
      .pipe(run)

  def queryBulkUpsertAndDeleteTheRest(itemLocations: Map[UUID, Option[Seq[Update]]], productId: UUID) =
    queryBulkUpsertAndDeleteTheRestByRelIds(
      itemLocations.values.flatten.flatten.toSeq,
      t =>
        t.productLocationId in productLocationDao
          .queryFindByItemIdsAndLocationIds(Seq(productId), itemLocations.keys.toSeq)
          .map(_.id),
    )

  def findByProductLocationId(productLocationId: UUID) =
    findByProductLocationIds(Seq(productLocationId))

  def findByProductLocationIds(productLocationIds: Seq[UUID]): Future[Seq[Record]] =
    if (productLocationIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByProductLocationIds(productLocationIds)
        .result
        .pipe(run)

  def findByTaxRateIds(taxRateIds: Seq[UUID]): Future[Seq[Record]] =
    if (taxRateIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByTaxRateIds(taxRateIds)
        .result
        .pipe(run)

  def findByProductLocationIdsAndTaxRateIds(productLocationIds: Seq[UUID], taxRateIds: Seq[UUID]): Future[Seq[Record]] =
    if (productLocationIds.isEmpty || taxRateIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByProductLocationIdsAndTaxRateIds(productLocationIds, taxRateIds)
        .result
        .pipe(run)
}
