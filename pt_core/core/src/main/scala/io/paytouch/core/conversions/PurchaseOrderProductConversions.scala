package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.entities._
import io.paytouch.core.entities.MonetaryAmount._

trait PurchaseOrderProductConversions {

  def fromRecordsAndExpansionsToEntities(
      records: Seq[PurchaseOrderProductRecord],
      productPerPurchaseOrderProduct: Map[PurchaseOrderProductRecord, Product],
      receivingOrderProductsPerPurchaseOrderProduct: Map[PurchaseOrderProductRecord, Seq[ReceivingOrderProduct]],
      quantityReturnedPerPurchaseOrderProduct: Map[PurchaseOrderProductRecord, BigDecimal],
      stockPerPurchaseOrderProduct: Map[PurchaseOrderProductRecord, BigDecimal],
    )(implicit
      user: UserContext,
    ) =
    records.flatMap { record =>
      productPerPurchaseOrderProduct.get(record).map { product =>
        val stock = stockPerPurchaseOrderProduct.getOrElse[BigDecimal](record, 0)
        val receivingOrderProducts = receivingOrderProductsPerPurchaseOrderProduct.getOrElse(record, Seq.empty)
        val receivingOrderProductCosts = receivingOrderProducts.flatMap(_.cost.map(_.amount))
        val receivedQuantity = receivingOrderProducts.flatMap(_.quantity).sum
        val receivedCostAverage: Option[MonetaryAmount] =
          if (receivingOrderProductCosts.nonEmpty)
            Some(MonetaryAmount(receivingOrderProductCosts.sum / receivingOrderProductCosts.size))
          else None
        val quantityReturned = quantityReturnedPerPurchaseOrderProduct.getOrElse[BigDecimal](record, 0)
        fromRecordAndExpansionToEntity(record, product, quantityReturned, stock, receivedQuantity, receivedCostAverage)
      }
    }

  def fromRecordAndExpansionToEntity(
      record: PurchaseOrderProductRecord,
      product: Product,
      quantityReturned: BigDecimal,
      stockLevel: BigDecimal,
      receivedQuantity: BigDecimal,
      receivedCostAverage: Option[MonetaryAmount],
    )(implicit
      user: UserContext,
    ) =
    PurchaseOrderProduct(
      productId = record.productId,
      productName = product.name,
      productUnit = product.unit,
      quantityOrdered = record.quantity.getOrElse[BigDecimal](0),
      quantityReceived = Some(receivedQuantity),
      quantityReturned = Some(quantityReturned),
      currentQuantity = stockLevel,
      averageCost = product.averageCost,
      orderedCost = MonetaryAmount.extract(record.costAmount),
      receivedCost = receivedCostAverage,
      options = product.options,
    )

  def fromUpsertionToUpdates(id: UUID, updates: Seq[PurchaseOrderProductUpsertion])(implicit user: UserContext) =
    updates.map(fromUpsertionToUpdate(id, _))

  def fromUpsertionToUpdate(id: UUID, update: PurchaseOrderProductUpsertion)(implicit user: UserContext) =
    PurchaseOrderProductUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      purchaseOrderId = Some(id),
      productId = Some(update.productId),
      quantity = update.quantity,
      costAmount = update.cost,
    )
}
