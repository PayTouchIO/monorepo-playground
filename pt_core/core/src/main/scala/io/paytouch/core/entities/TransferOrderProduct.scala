package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.UnitType
import io.paytouch.core.entities.enums.ExposedName

final case class TransferOrderProduct(
    productId: UUID,
    productName: String,
    productUnit: UnitType,
    transferQuantity: BigDecimal,
    fromCurrentQuantity: BigDecimal,
    toCurrentQuantity: BigDecimal,
    totalValue: MonetaryAmount,
    options: Seq[VariantOptionWithType],
  ) extends ExposedEntity {
  val classShortName = ExposedName.TransferOrderProduct
}

final case class TransferOrderProductUpsertion(productId: UUID, quantity: Option[BigDecimal])
