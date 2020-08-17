package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.UnitType
import io.paytouch.core.entities.enums.ExposedName

final case class ReceivingOrderProduct(
    productId: UUID,
    receivingOrderId: UUID,
    quantity: Option[BigDecimal],
    cost: Option[MonetaryAmount],
  )

final case class ReceivingOrderProductUpsertion(
    productId: UUID,
    quantity: Option[BigDecimal],
    cost: Option[BigDecimal],
  )

final case class ReceivingOrderProductDetails(
    productId: UUID,
    productName: String,
    productUnit: UnitType,
    quantityOrdered: Option[BigDecimal],
    quantityReceived: BigDecimal,
    currentQuantity: BigDecimal,
    averageCost: Option[MonetaryAmount],
    orderedCost: Option[MonetaryAmount],
    receivedCost: MonetaryAmount,
    totalValue: MonetaryAmount,
    options: Seq[VariantOptionWithType],
  ) extends ExposedEntity {
  val classShortName = ExposedName.ReceivingOrderProductDetails
}
