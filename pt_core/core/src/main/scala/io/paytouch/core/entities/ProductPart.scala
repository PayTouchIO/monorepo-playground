package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class ProductPart(part: Product, quantityNeeded: BigDecimal) extends ExposedEntity {
  val classShortName = ExposedName.ProductPart
}

final case class ProductPartAssignment(partId: UUID, quantityNeeded: BigDecimal)
