package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.{ ReturnOrderReason, UnitType }
import io.paytouch.core.entities.enums.ExposedName

final case class ReturnOrderProduct(
    productId: UUID,
    productName: String,
    productUnit: UnitType,
    reason: ReturnOrderReason,
    quantity: Option[BigDecimal],
    currentQuantity: BigDecimal,
    options: Seq[VariantOptionWithType],
  ) extends ExposedEntity {
  val classShortName = ExposedName.ReturnOrderProduct
}

final case class ReturnOrderProductUpsertion(
    productId: UUID,
    reason: ReturnOrderReason,
    quantity: Option[BigDecimal],
  )
