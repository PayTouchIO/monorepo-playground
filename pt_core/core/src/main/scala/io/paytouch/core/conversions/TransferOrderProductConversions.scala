package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.entities.{ TransferOrderUpdate => TransferOrderUpdateEntity, _ }

import scala.concurrent._

trait TransferOrderProductConversions {

  def fromRecordsAndExpansionsToEntities(
      records: Seq[TransferOrderProductRecord],
      productPerTransferOrderProduct: Map[TransferOrderProductRecord, Product],
      fromCurrentQuantityPerTransferOrderProduct: Map[TransferOrderProductRecord, BigDecimal],
      toCurrentQuantityPerTransferOrderProduct: Map[TransferOrderProductRecord, BigDecimal],
      totalValuePerTransferOrderProduct: Map[TransferOrderProductRecord, MonetaryAmount],
    )(implicit
      user: UserContext,
    ) =
    records.flatMap { record =>
      productPerTransferOrderProduct.get(record).map { product =>
        val fromCurrentQuantity = fromCurrentQuantityPerTransferOrderProduct.getOrElse[BigDecimal](record, 0)
        val toCurrentQuantity = toCurrentQuantityPerTransferOrderProduct.getOrElse[BigDecimal](record, 0)
        val totalValue = {
          val zeroMonetary = MonetaryAmount(0, user.currency)
          totalValuePerTransferOrderProduct.getOrElse(record, zeroMonetary)
        }
        fromRecordAndExpansionToEntity(record, product, fromCurrentQuantity, toCurrentQuantity, totalValue)
      }
    }

  def fromRecordAndExpansionToEntity(
      record: TransferOrderProductRecord,
      product: Product,
      fromCurrentQuantity: BigDecimal,
      toCurrentQuantity: BigDecimal,
      totalValue: MonetaryAmount,
    ) =
    TransferOrderProduct(
      productId = record.productId,
      productName = record.productName,
      productUnit = record.productUnit,
      transferQuantity = record.quantity.getOrElse[BigDecimal](0),
      fromCurrentQuantity = fromCurrentQuantity,
      toCurrentQuantity = toCurrentQuantity,
      totalValue = totalValue,
      options = product.options,
    )

  def fromUpsertionToUpdates(
      id: UUID,
      updates: Seq[TransferOrderProductUpsertion],
      productsByProductId: Map[UUID, ArticleRecord],
    )(implicit
      user: UserContext,
    ) =
    updates.flatMap { update =>
      val product = productsByProductId.get(update.productId)
      product.map(fromUpsertionToUpdate(id, update, _))
    }

  private def fromUpsertionToUpdate(
      transferOrderId: UUID,
      transferOrderProduct: TransferOrderProductUpsertion,
      product: ArticleRecord,
    )(implicit
      user: UserContext,
    ): TransferOrderProductUpdate =
    TransferOrderProductUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      transferOrderId = Some(transferOrderId),
      productId = Some(transferOrderProduct.productId),
      productName = Some(product.name),
      productUnit = Some(product.unit),
      quantity = transferOrderProduct.quantity,
    )
}
