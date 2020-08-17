package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickRelDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ModifierSetProductRecord, ModifierSetProductUpdate }
import io.paytouch.core.data.tables.ModifierSetProductsTable

class ModifierSetProductDao(
    modifierSetDao: => ModifierSetDao,
    val modifierSetLocationDao: ModifierSetLocationDao,
    articleDao: => ArticleDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends SlickRelDao {
  type Record = ModifierSetProductRecord
  type Update = ModifierSetProductUpdate
  type Table = ModifierSetProductsTable

  val table = TableQuery[Table]

  def countByModifierSetIds(modifierSetIds: Seq[UUID]): Future[Map[UUID, Int]] =
    if (modifierSetIds.isEmpty)
      Future.successful(Map.empty)
    else
      table
        .filter(_.modifierSetId inSet modifierSetIds)
        .join(articleDao.nonDeletedTable)
        .on(_.productId === _.id)
        .groupBy { case (modifierSetProductTable, articleTable) => modifierSetProductTable.modifierSetId }
        .map {
          case (ms, results) => ms -> results.length
        }
        .result
        .pipe(run)
        .map(_.toMap)

  def queryFindByModifierSetIdAndProductId(modifierSetId: UUID, productId: UUID) =
    table.filter(t => t.modifierSetId === modifierSetId && t.productId === productId)

  def queryByRelIds(modifierSetProduct: Update) = {
    require(
      modifierSetProduct.modifierSetId.isDefined,
      "CategoryLocationDao - Impossible to find by modifier set id and location id without a modifier set id",
    )

    require(
      modifierSetProduct.productId.isDefined,
      "CategoryLocationDao - Impossible to find by modifier set id and product id without a product id",
    )

    queryFindByModifierSetIdAndProductId(modifierSetProduct.modifierSetId.get, modifierSetProduct.productId.get)
  }

  def queryBulkUpsertAndDeleteTheRestByModifierSetIds(modifierSetProducts: Seq[Update], modifierSetIds: Seq[UUID]) =
    for {
      oldProductIds <- queryFindByModifierSetIds(modifierSetIds).map(_.productId).result
      newProductIds = modifierSetProducts.flatMap(_.productId)
      updates <- queryBulkUpsertAndDeleteTheRestByRelIds(modifierSetProducts, _.modifierSetId inSet modifierSetIds)
      _ <- articleDao.queryMarkAsUpdatedByIds(oldProductIds ++ newProductIds)
      _ <- modifierSetDao.queryMarkAsUpdatedByIds(modifierSetIds)
    } yield updates

  def queryBulkUpsertAndDeleteTheRestByProductIds(modifierSetProducts: Seq[Update], productIds: Seq[UUID]) =
    queryBulkUpsertAndDeleteTheRestByRelIds(modifierSetProducts, t => t.productId inSet productIds)

  def queryFindByProductIds(productIds: Seq[UUID]) =
    table.filter(_.productId inSet productIds).result

  def queryFindByModifierSetIds(modifierSetIds: Seq[UUID]) =
    table.filter(_.modifierSetId inSet modifierSetIds)

  def queryFindByModifierSetId(modifierSetId: UUID) =
    queryFindByModifierSetIds(Seq(modifierSetId))

  def bulkUpsertAndDeleteTheRestByProductIds(modifierSetProducts: Seq[Update], productIds: Seq[UUID]) =
    (for {
      oldModifierSetIds <- queryFindByProductIds(productIds).map(_.map(_.modifierSetId))
      newModifierSetIds = modifierSetProducts.flatMap(_.modifierSetId)
      updates <- queryBulkUpsertAndDeleteTheRestByProductIds(modifierSetProducts, productIds)
      _ <- modifierSetDao.queryMarkAsUpdatedByIds(oldModifierSetIds ++ newModifierSetIds)
      _ <- articleDao.queryMarkAsUpdatedByIds(productIds)
    } yield updates).pipe(runWithTransaction)

  def bulkUpsertAndDeleteTheRestByModifierSetIds(modifierSetProducts: Seq[Update], modifierSetIds: Seq[UUID]) =
    runWithTransaction(queryBulkUpsertAndDeleteTheRestByModifierSetIds(modifierSetProducts, modifierSetIds))

  def findByProductIds(productIds: Seq[UUID]): Future[Seq[Record]] =
    if (productIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByProductIds(productIds)
        .pipe(run)

  def findByProductId(productId: UUID) =
    findByProductIds(Seq(productId))

  def findByModifierSetId(modifierSetId: UUID) =
    findByModifierSetIds(Seq(modifierSetId))

  def findByModifierSetIds(modifierSetIds: Seq[UUID]): Future[Seq[Record]] =
    if (modifierSetIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByModifierSetIds(modifierSetIds)
        .result
        .pipe(run)

  def queryFindByModifierSetIdAndOptionalLocationIds(modifierSetId: UUID, locationIds: Option[Seq[UUID]]) = {
    val query = queryFindByModifierSetId(modifierSetId)
    locationIds.fold(query) { locationIds =>
      query
        .join(modifierSetLocationDao.queryFindByLocationIds(locationIds))
        .on(_.modifierSetId === _.modifierSetId)
        .map {
          case (modifierSetProductTable, _) => modifierSetProductTable
        }
    }
  }
}
