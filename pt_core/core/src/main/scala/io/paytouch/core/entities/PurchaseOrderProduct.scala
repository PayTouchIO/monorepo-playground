package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.UnitType
import io.paytouch.core.entities.enums.ExposedName

final case class PurchaseOrderProduct(
    productId: UUID,
    productName: String,
    productUnit: UnitType,
    quantityOrdered: BigDecimal,
    quantityReceived: Option[BigDecimal],
    quantityReturned: Option[BigDecimal],
    currentQuantity: BigDecimal,
    averageCost: Option[MonetaryAmount],
    orderedCost: Option[MonetaryAmount],
    receivedCost: Option[MonetaryAmount],
    options: Seq[VariantOptionWithType],
  ) extends ExposedEntity {
  val classShortName = ExposedName.PurchaseOrderProduct
}

final case class PurchaseOrderProductUpsertion(
    productId: UUID,
    quantity: Option[BigDecimal],
    cost: Option[BigDecimal],
  )
