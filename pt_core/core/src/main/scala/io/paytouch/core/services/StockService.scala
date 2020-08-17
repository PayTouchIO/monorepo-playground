package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.async.monitors._
import io.paytouch.core.conversions.StockConversions
import io.paytouch.core.data.daos.{ Daos, StockDao }
import io.paytouch.core.data.model.enums.ArticleScope
import io.paytouch.core.data.model.{
  ArticleRecord,
  ProductLocationRecord,
  StockRecord,
  StockUpdate => StockUpdateModel,
}
import io.paytouch.core.entities.{ UserContext, Stock => StockEntity, StockUpdate => StockUpdateEntity }
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.StockFilters
import io.paytouch.core.services.features.{ BulkCreateAndUpdateFeatureWithStateProcessing, FindAllFeature }
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.StockValidator
import io.paytouch.core.withTag

class StockService(
    val productQHMonitor: ActorRef withTag ProductQuantityHistoryMonitor,
    val stockModifierMonitor: ActorRef withTag StockModifierMonitor,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends StockConversions
       with FindAllFeature
       with BulkCreateAndUpdateFeatureWithStateProcessing {
  type Dao = StockDao
  type Entity = StockEntity
  type Expansions = NoExpansions
  type Filters = StockFilters
  type Model = StockUpdateModel
  type Record = StockRecord
  type Update = StockUpdateEntity
  type Validator = StockValidator

  protected val dao = daos.stockDao
  protected val validator = new StockValidator

  val articleDao = daos.articleDao
  val productLocationDao = daos.productLocationDao

  type State = (Seq[Record], Seq[ProductLocationRecord])

  def enrich(
      records: Seq[Record],
      filters: Filters,
    )(
      expansions: Expansions,
    )(implicit
      user: UserContext,
    ): Future[Seq[Entity]] =
    Future.successful(toSeqEntity(records))

  def convertToUpsertionModel(updates: Seq[Update])(implicit user: UserContext): Future[ErrorsOr[Seq[Model]]] =
    validator.validateStockUpsertions(updates).mapNested(_ => fromUpdateEntitiesToModels(updates))

  def findAllPerProduct(
      products: Seq[ArticleRecord],
      locationIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[Map[ArticleRecord, Seq[Record]]] = {
    val productIds = products.map(_.id)
    dao.findAllByProductIds(user.merchantId, productIds, locationIds).map { records =>
      records.groupBy(_.productId).mapKeysToRecords(products)
    }
  }

  def findAllPerProductIdsAndLocationId(
      productIds: Seq[UUID],
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): Future[Map[(UUID, UUID), Record]] =
    findAllPerProductIdsAndLocationIds(productIds, Seq(locationId))

  def findAllPerProductIdsAndLocationIds(
      productIds: Seq[UUID],
      locationIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[(UUID, UUID), Record]] =
    dao.findByProductIdsAndLocationIds(productIds, locationIds).map { records =>
      records.map(r => ((r.productId, r.locationId), r)).toMap
    }

  def findStockByArticleIds(
      productIds: Seq[UUID],
      locationIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Map[UUID, Entity]]] =
    dao.findByProductIdsAndLocationIds(productIds, user.accessibleLocations(locationIds)).map { records =>
      toSeqEntity(records)
        .groupBy(_.productId)
        .map { case (productId, results) => productId -> results.map(r => r.locationId -> r).toMap }
    }

  def findStockLevelByVariantArticleIds(
      productIds: Seq[UUID],
      locationIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Map[UUID, BigDecimal]]] =
    dao.findStockLevelByVariantProductIds(productIds, user.accessibleLocations(locationIds))

  def findStockLevelByArticleIds(
      productIds: Seq[UUID],
      locationIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Map[UUID, BigDecimal]]] =
    dao.findStockLevelByProductsIds(productIds, user.accessibleLocations(locationIds))

  def findReorderAmountByArticleIds(
      productIds: Seq[UUID],
      locationIds: Option[Seq[UUID]],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Map[UUID, BigDecimal]]] =
    dao.findReorderAmountByProductsIds(productIds, user.accessibleLocations(locationIds))

  def saveCurrentState(stockRecords: Seq[Record])(implicit user: UserContext): Future[State] = {
    val productIds = stockRecords.map(_.productId)
    val locationIds = stockRecords.map(_.locationId)
    productLocationDao.findByProductIdsAndLocationIds(productIds = productIds, locationIds = locationIds).map {
      productLocationRecords => (stockRecords.sortBy(_.id), productLocationRecords.sortBy(_.id))
    }
  }

  def processChangeOfState(
      state: State,
      updates: Seq[Update],
      resultType: ResultType,
      entities: Seq[Entity],
    )(implicit
      user: UserContext,
    ): Future[Unit] = {
    recordProductQuantityHistoryChange(state, updates)
    triggerPartStockModifierChanges(state, updates)
  }

  private def recordProductQuantityHistoryChange(
      state: State,
      updates: Seq[Update],
    )(implicit
      user: UserContext,
    ): Unit = {
    val (stocks, productLocations) = state

    for {
      update <- updates
      maybePreviousStock = stocks.find(_.contains(update.productId, update.locationId))
      productLocation <- productLocations.filter(_.contains(update.productId, update.locationId))
    } yield {
      val prevQuantity =
        maybePreviousStock
          .map(_.quantity)
          .getOrElse[BigDecimal](0)

      val msg = ProductQuantityIncrease(
        productLocation = productLocation,
        prevQuantity = prevQuantity,
        newQuantity = update.quantity.getOrElse(prevQuantity),
        orderId = None,
        reason = update.reason,
        notes = update.notes,
        userContext = user,
      )

      productQHMonitor ! msg
    }
  }

  private def triggerPartStockModifierChanges(
      state: State,
      updates: Seq[Update],
    )(implicit
      user: UserContext,
    ): Future[Unit] = {
    val (stocks, _) = state

    articleDao.findByIds(updates.map(_.productId)).map { articles =>
      updates.groupBy(_.productId).foreach {
        case (articleId, upds) =>
          for {
            article <- articles.filter(_.id == articleId)
            if article.scope == ArticleScope.Part
            update <- upds
          } yield {
            val maybeStock = stocks.find(_.contains(update.productId, update.locationId))

            val locationQuantitiesPerArticle: Map[ArticleRecord, Map[UUID, BigDecimal]] = {
              val newQuantity = update.quantity.getOrElse[BigDecimal](0)
              val oldQuantity = maybeStock.map(_.quantity).getOrElse[BigDecimal](0)
              val diffQuantity = newQuantity - oldQuantity

              if (diffQuantity > 0)
                Map(article -> Map(update.locationId -> -diffQuantity))
              else
                Map.empty
            }

            val msg = StockPartModifierChanges(
              locationQuantitiesPerArticle = locationQuantitiesPerArticle,
              orderId = None,
              reason = Some(update.reason),
              notes = update.notes,
              userContext = user,
            )

            stockModifierMonitor ! msg
          }
      }
    }
  }

  def createEmptyStocks(
      productsWithLocations: Map[UUID, UUID],
    )(implicit
      user: UserContext,
    ): Future[Map[(UUID, UUID), Record]] = {
    val updates = productsWithLocations.map {
      case (productId, locationId) => toEmptyStockUpdateModel(productId, locationId)
    }.toSeq
    dao
      .bulkUpsert(updates)
      .map(results =>
        results.map {
          case (_, record) =>
            (record.productId, record.locationId) -> record
        }.toMap,
      )
  }

}
