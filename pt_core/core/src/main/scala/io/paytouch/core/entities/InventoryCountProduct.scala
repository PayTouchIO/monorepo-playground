package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.UnitType
import io.paytouch.core.entities.enums.ExposedName

final case class InventoryCountProduct(
    productId: UUID,
    productName: String,
    productUnit: UnitType,
    productCost: Option[MonetaryAmount],
    expectedQuantity: Option[BigDecimal],
    countedQuantity: Option[BigDecimal],
    value: Option[MonetaryAmount],
    options: Seq[VariantOptionWithType],
  ) extends ExposedEntity {
  val classShortName = ExposedName.InventoryCountProduct
}

final case class InventoryCountProductUpsertion(
    productId: UUID,
    expectedQuantity: Option[BigDecimal],
    countedQuantity: Option[BigDecimal],
  )
