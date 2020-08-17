package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

/**
  * totalSold and totalRevenue are not used anymore.
  * They are initialized with the default values so that the REST API stays backwards compatible.
  */
final case class Inventory(
    id: UUID,
    name: String,
    upc: Option[String],
    sku: Option[String],
    isVariantOfProductId: Option[UUID],
    options: Seq[VariantOptionWithType],
    totalQuantity: ProductQuantity,
    totalSold: ProductQuantity = ProductQuantity.Zero,
    totalRevenue: Seq[MonetaryAmount] = Seq.empty,
    stockValue: MonetaryAmount,
  ) extends ExposedEntity {
  val classShortName = ExposedName.ProductInventory
}
