package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.core.async.monitors.{ StockModifierChange, StockModifierMonitor, StockPartModifierChanges }
import io.paytouch.core.calculations._
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.{ OrderStatus, PaymentStatus }
import io.paytouch.core.data.model.upsertions.{
  OrderUpsertion => OrderUpsertionModel,
  OrderItemUpsertion => OrderItemUpsertionModel,
}
import io.paytouch.core.entities.{ Order, UserContext }
import io.paytouch.core.withTag

class StockModifierService(
    monitor: ActorRef withTag StockModifierMonitor,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends StockModifierCalculations {
  final type State = (Seq[StockRecord], Seq[ProductLocationRecord])
  final type StockChange = (ArticleRecord, BigDecimal, LocationId)
  final type LocationId = UUID

  val articleDao = daos.articleDao

  def modifyStocks(
      orderId: UUID,
      upsertionModel: OrderUpsertionModel,
      oldOrder: Option[Order],
      oldOrderItems: Seq[OrderItemRecord],
      oldOrderBundles: Seq[OrderBundleRecord],
    )(implicit
      user: UserContext,
    ): Future[Unit] =
    calculateStockChanges(
      upsertionModel,
      oldOrder.flatMap(_.status),
      oldOrderItems,
      oldOrderBundles,
    ).map(processQuantityChanges(_, orderId.some))

  // See also calculateStockChanges in OrderInventoryValidator which should follow the same logic
  private def calculateStockChanges(
      upsertionModel: OrderUpsertionModel,
      oldOrderStatus: Option[OrderStatus],
      oldOrderItems: Seq[OrderItemRecord],
      oldOrderBundles: Seq[OrderBundleRecord],
    )(implicit
      user: UserContext,
    ): Future[Seq[StockChange]] = {
    val orderItems: Seq[OrderItemUpdate] = upsertionModel.orderItems.map(_.orderItem)

    articleDao
      .findByIds(allProductIds(oldOrderItems, orderItems.flatMap(_.productId)))
      .map { products =>
        (products.toList, upsertionModel.order.locationId.toList).mapN { (product, locationId) =>
          val diff =
            calculateDiff(product.id, upsertionModel, oldOrderStatus, oldOrderItems, oldOrderBundles, orderItems)

          (product, diff, locationId)
        }
      }
  }

  private def processQuantityChanges(changes: Seq[StockChange], orderId: Option[UUID])(implicit user: UserContext) = {
    val dataPerArticle: Map[ArticleRecord, Map[UUID, BigDecimal]] =
      changes
        .groupBy {
          case (product, _, _) => product
        }
        .transform { (_, change) =>
          change.map {
            case (_, quantity, locationId) => locationId -> quantity
          }.toMap
        }

    val decreasingOnlyDataPerArticle: Map[ArticleRecord, Map[UUID, BigDecimal]] =
      dataPerArticle.transform { (_, change) =>
        change.filter {
          case (_, quantity) => quantity < 0
        }
      }

    monitor ! StockModifierChange(dataPerArticle, orderId, reason = None, notes = None, user)
    monitor ! StockPartModifierChanges(decreasingOnlyDataPerArticle, orderId, reason = None, notes = None, user)
  }
}
