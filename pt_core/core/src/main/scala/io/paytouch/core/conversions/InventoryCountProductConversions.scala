package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ InventoryCountProductUpdate => InventoryCountProductUpdateModel, _ }
import io.paytouch.core.entities._

trait InventoryCountProductConversions {

  def fromRecordsAndExpansionsToEntities(
      records: Seq[InventoryCountProductRecord],
      productPerInventoryCountProduct: Map[InventoryCountProductRecord, Product],
    )(implicit
      userContext: UserContext,
    ) =
    records.flatMap { record =>
      productPerInventoryCountProduct.get(record).map(product => fromRecordAndExpansionToEntity(record, product))
    }

  def fromRecordAndExpansionToEntity(
      record: InventoryCountProductRecord,
      product: Product,
    )(implicit
      userContext: UserContext,
    ) =
    InventoryCountProduct(
      productId = product.id,
      productName = product.name,
      productUnit = product.unit,
      productCost = MonetaryAmount.extract(record.costAmount),
      expectedQuantity = record.expectedQuantity,
      countedQuantity = record.countedQuantity,
      value = MonetaryAmount.extract(record.valueAmount),
      options = product.options,
    )

  def fromUpsertionToUpdates(
      id: UUID,
      locationId: UUID,
      updates: Seq[InventoryCountProductUpsertion],
      productsByProductId: Map[UUID, Product],
    )(implicit
      user: UserContext,
    ) =
    updates.flatMap { update =>
      for {
        product <- productsByProductId.get(update.productId)
        productLocation <- product.locationOverrides.get(locationId)
      } yield fromUpsertionToUpdate(id, update, product, productLocation)
    }

  def fromUpsertionToUpdate(
      id: UUID,
      update: InventoryCountProductUpsertion,
      product: Product,
      productLocation: ProductLocation,
    )(implicit
      user: UserContext,
    ) = {
    val costAmount = productLocation.cost.map(_.amount)
    val valueAmount = for {
      counted <- update.countedQuantity
      costAmount <- costAmount
    } yield costAmount * counted

    val valueChangeAmount = for {
      expected <- update.expectedQuantity
      counted <- update.countedQuantity
      costAmount <- costAmount
    } yield costAmount * (counted - expected)

    InventoryCountProductUpdateModel(
      id = None,
      merchantId = Some(user.merchantId),
      inventoryCountId = Some(id),
      productId = Some(update.productId),
      productName = Some(product.name),
      expectedQuantity = update.expectedQuantity,
      countedQuantity = update.countedQuantity,
      valueAmount = valueAmount,
      costAmount = costAmount,
      valueChangeAmount = valueChangeAmount,
    )
  }
}
