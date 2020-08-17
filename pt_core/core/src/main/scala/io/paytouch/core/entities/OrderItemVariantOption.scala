package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class OrderItemVariantOption(
    id: UUID,
    orderItemId: UUID,
    variantOptionId: Option[UUID],
    optionName: String,
    optionTypeName: String,
    position: Int,
  ) extends ExposedEntity {
  val classShortName = ExposedName.OrderItemVariantOption
}

final case class OrderItemVariantOptionUpsertion(
    variantOptionId: Option[UUID], // Temp HOT-FIX for bug register PR-1488
    optionName: Option[String],
    optionTypeName: Option[String],
    position: Option[Int],
  )
