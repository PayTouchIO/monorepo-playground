package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.QuantityChangeReason
import io.paytouch.core.entities.enums.ExposedName

final case class Stock(
    id: UUID,
    locationId: UUID,
    productId: UUID,
    quantity: BigDecimal,
    minimumOnHand: BigDecimal,
    reorderAmount: BigDecimal,
    sellOutOfStock: Boolean,
  ) extends ExposedEntity {
  val classShortName = ExposedName.Stock
}

final case class StockCreation(
    locationId: UUID,
    productId: UUID,
    quantity: BigDecimal,
    minimumOnHand: BigDecimal = 0,
    reorderAmount: BigDecimal,
    sellOutOfStock: Option[Boolean],
  ) extends CreationEntityWithRelIds[Stock, StockUpdate] {
  val relId1: UUID = productId
  val relId2: UUID = locationId

  def asUpdate =
    StockUpdate(
      locationId = locationId,
      productId = productId,
      quantity = Some(quantity),
      minimumOnHand = Some(minimumOnHand),
      reorderAmount = Some(reorderAmount),
      sellOutOfStock = sellOutOfStock,
      reason = QuantityChangeReason.Manual,
      notes = None,
    )
}

final case class StockUpdate(
    locationId: UUID,
    productId: UUID,
    quantity: Option[BigDecimal],
    minimumOnHand: Option[BigDecimal],
    reorderAmount: Option[BigDecimal],
    sellOutOfStock: Option[Boolean],
    reason: QuantityChangeReason = QuantityChangeReason.Manual,
    notes: Option[String],
  ) extends UpdateEntityWithRelIds[Stock] {
  val relId1: UUID = productId
  val relId2: UUID = locationId
}
