package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.calculations.CalculationUtils
import io.paytouch.core.data.model.{
  ArticleRecord,
  ReceivingOrderProductRecord,
  ReceivingOrderProductUpdate => ReceivingOrderProductUpdateModel,
}
import io.paytouch.core.entities._

trait ReceivingOrderProductConversions extends EntityConversion[ReceivingOrderProductRecord, ReceivingOrderProduct] {
  import CalculationUtils._

  def fromRecordToEntity(record: ReceivingOrderProductRecord)(implicit user: UserContext): ReceivingOrderProduct =
    ReceivingOrderProduct(
      productId = record.productId,
      receivingOrderId = record.receivingOrderId,
      quantity = record.quantity,
      cost = MonetaryAmount.extract(record.costAmount),
    )

  def fromUpsertionToUpdates(
      id: UUID,
      updates: Seq[ReceivingOrderProductUpsertion],
      productsByProductId: Map[UUID, ArticleRecord],
    )(implicit
      user: UserContext,
    ) =
    updates.flatMap { update =>
      val product = productsByProductId.get(update.productId)
      product.map(fromUpsertionToUpdate(id, update, _))
    }

  def fromUpsertionToUpdate(
      id: UUID,
      update: ReceivingOrderProductUpsertion,
      product: ArticleRecord,
    )(implicit
      user: UserContext,
    ) =
    ReceivingOrderProductUpdateModel(
      id = None,
      merchantId = Some(user.merchantId),
      receivingOrderId = Some(id),
      productId = Some(update.productId),
      productName = Some(product.name),
      productUnit = Some(product.unit),
      quantity = update.quantity,
      costAmount = update.cost,
    )

  def toReceivingOrderProductDetails(
      receivingOrderProducts: Seq[ReceivingOrderProductRecord],
      productsPerProductId: Map[UUID, Product],
      purchaseOrderProductsPerProductId: Map[UUID, Seq[PurchaseOrderProduct]],
      transferOrderProductsPerProductId: Map[UUID, Seq[TransferOrderProduct]],
      stockLevelPerProductId: Map[UUID, BigDecimal],
    )(implicit
      user: UserContext,
    ): Seq[ReceivingOrderProductDetails] =
    receivingOrderProducts.flatMap { receivingOrderProduct =>
      productsPerProductId.get(receivingOrderProduct.productId).map { product =>
        val purchaseOrderProducts = purchaseOrderProductsPerProductId.get(receivingOrderProduct.productId)
        val transferOrderProducts = transferOrderProductsPerProductId.get(receivingOrderProduct.productId)
        val stockLevel = stockLevelPerProductId.getOrElse[BigDecimal](receivingOrderProduct.productId, 0)
        toEntityDetails(receivingOrderProduct, product, purchaseOrderProducts, transferOrderProducts, stockLevel)
      }
    }

  private def toEntityDetails(
      receivingOrderProduct: ReceivingOrderProductRecord,
      product: Product,
      purchaseOrderProducts: Option[Seq[PurchaseOrderProduct]],
      transferOrderProducts: Option[Seq[TransferOrderProduct]],
      stockLevel: BigDecimal,
    )(implicit
      user: UserContext,
    ): ReceivingOrderProductDetails = {
    val orderedCost = purchaseOrderProducts.map(_.flatMap(_.orderedCost).sumNonZero)

    val receivedCost = MonetaryAmount(receivingOrderProduct.costAmount.getOrElse[BigDecimal](0))
    val quantityReceived = receivingOrderProduct.quantity.getOrElse[BigDecimal](0)
    val quantityOrdered = {
      def purchaseOrderQuantity = purchaseOrderProducts.map(_.map(_.quantityOrdered).sum)
      def transferOrderQuantity = transferOrderProducts.map(_.map(_.transferQuantity).sum)
      purchaseOrderQuantity orElse transferOrderQuantity
    }

    ReceivingOrderProductDetails(
      productId = receivingOrderProduct.productId,
      productName = receivingOrderProduct.productName,
      productUnit = receivingOrderProduct.productUnit,
      quantityOrdered = quantityOrdered,
      quantityReceived = quantityReceived,
      currentQuantity = stockLevel,
      averageCost = product.averageCost,
      orderedCost = orderedCost,
      receivedCost = receivedCost,
      totalValue = receivedCost * quantityReceived,
      options = product.options,
    )
  }
}
