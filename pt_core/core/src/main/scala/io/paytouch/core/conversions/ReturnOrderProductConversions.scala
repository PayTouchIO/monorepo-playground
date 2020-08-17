package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.entities.{ ReturnOrderUpdate => ReturnOrderUpdateEntity, _ }

import scala.concurrent._

trait ReturnOrderProductConversions {

  def fromRecordsAndExpansionsToEntities(
      records: Seq[ReturnOrderProductRecord],
      productPerReturnOrderProduct: Map[ReturnOrderProductRecord, Product],
      stockPerReturnOrderProduct: Map[ReturnOrderProductRecord, BigDecimal],
    ) =
    records.flatMap { record =>
      productPerReturnOrderProduct.get(record).map { product =>
        val stock = stockPerReturnOrderProduct.getOrElse[BigDecimal](record, 0)
        fromRecordAndExpansionToEntity(record, product, stock)
      }
    }

  def fromRecordAndExpansionToEntity(
      record: ReturnOrderProductRecord,
      product: Product,
      stockLevel: BigDecimal,
    ) =
    ReturnOrderProduct(
      productId = record.productId,
      productName = product.name,
      productUnit = product.unit,
      reason = record.reason,
      quantity = record.quantity,
      currentQuantity = stockLevel,
      options = product.options,
    )

  def fromUpsertionToUpdates(
      id: UUID,
      updates: Seq[ReturnOrderProductUpsertion],
      productsByProductId: Map[UUID, ArticleRecord],
    )(implicit
      user: UserContext,
    ) =
    updates.flatMap { update =>
      val product = productsByProductId.get(update.productId)
      product.map(fromUpsertionToUpdate(id, update, _))
    }

  private def fromUpsertionToUpdate(
      returnOrderId: UUID,
      returnOrderProduct: ReturnOrderProductUpsertion,
      product: ArticleRecord,
    )(implicit
      user: UserContext,
    ): ReturnOrderProductUpdate =
    ReturnOrderProductUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      returnOrderId = Some(returnOrderId),
      productId = Some(returnOrderProduct.productId),
      productName = Some(product.name),
      productUnit = Some(product.unit),
      quantity = returnOrderProduct.quantity,
      reason = Some(returnOrderProduct.reason),
    )
}
