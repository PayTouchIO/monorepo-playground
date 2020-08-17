package io.paytouch.core.validators

import java.util.UUID

import scala.concurrent._

import io.paytouch.core.calculations._
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ ArticleRecord, OrderBundleRecord, OrderItemRecord, OrderRecord }
import io.paytouch.core.entities._
import io.paytouch.core.errors.ProductOutOfStock
import io.paytouch.core.utils.{ Multiple, PaytouchLogger }
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.data.model.enums.OrderStatus

class OrderInventoryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends StockModifierCalculations {
  final type StockChange = (ArticleRecord, BigDecimal)

  val orderItemDao = daos.orderItemDao
  val orderBundleDao = daos.orderBundleDao
  val articleDao = daos.articleDao
  val stockDao = daos.stockDao

  def validateUpsertions(
      orderId: UUID,
      upsertion: OrderUpsertion,
      existingOrder: Option[OrderRecord],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[OrderUpsertion]] = {
    val orderItemIds = upsertion.items.map(_.id)
    val orderId = existingOrder.map(_.id).getOrElse(UUID.randomUUID)

    for {
      existingOrderItems <- orderItemDao.findByIds(orderItemIds)
      existingOrderBundles <- orderBundleDao.findByOrderId(orderId)
      changes <- calculateStockChanges(
        upsertion,
        existingOrder.flatMap(_.status),
        existingOrderItems,
        existingOrderBundles,
      )
      validChanges <- validateChanges(orderId, changes, upsertion)
    } yield validChanges.map(_ => upsertion)
  }

  // See also calculateStockChanges in stockModifierService which should follow the same logic
  private def calculateStockChanges(
      upsertion: OrderUpsertion,
      oldOrderStatus: Option[OrderStatus],
      oldOrderItems: Seq[OrderItemRecord],
      oldOrderBundles: Seq[OrderBundleRecord],
    )(implicit
      user: UserContext,
    ): Future[Seq[StockChange]] = {
    val orderItems: Seq[OrderItemUpsertion] = upsertion.items

    articleDao
      .findByIds(allProductIds(oldOrderItems, orderItems.flatMap(_.productId)))
      .map { products =>
        products
          .filter(_.trackInventory)
          .map { product =>
            val diff =
              calculateDiff(product.id, upsertion, oldOrderStatus, oldOrderItems, oldOrderBundles, orderItems)

            product -> diff
          }
      }
  }

  private def validateChanges(
      orderId: UUID,
      changes: Seq[StockChange],
      upsertion: OrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[Unit]]] = {
    val articleIds = changes.map {
      case (article, _) => article.id
    }

    stockDao
      .findByProductIdsAndLocationIds(articleIds, Seq(upsertion.locationId))
      .map { stocks =>
        val result: ErrorsOr[Seq[Unit]] =
          Multiple.combineSeq(
            changes.map {
              case (article, orderQuantity) =>
                val stock =
                  stocks.find(_.productId == article.id)

                logger.info(
                  s"[flaky][ProductOutOfStock debugging] articleId ${article.id} orderQuantity $orderQuantity stock $stock",
                )

                stock match {
                  case Some(s) if s.quantity < orderQuantity && !s.sellOutOfStock =>
                    Multiple.failure(ProductOutOfStock(article.id))

                  case _ =>
                    Multiple.success(())
                }
            },
          )

        logger.info(s"[flaky][ProductOutOfStock debugging] $result")

        result
      }
  }
}
