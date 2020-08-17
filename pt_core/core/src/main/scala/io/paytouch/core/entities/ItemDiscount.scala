package io.paytouch.core.entities

import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.enums.ExposedName

final case class OrderItemDiscount(
    id: UUID,
    orderItemId: UUID,
    discountId: Option[UUID],
    title: Option[String],
    `type`: DiscountType,
    amount: BigDecimal,
    totalAmount: Option[BigDecimal],
    currency: Option[Currency],
  ) extends ExposedEntity {
  val classShortName = ExposedName.OrderItemDiscount
}

final case class OrderDiscount(
    id: UUID,
    orderId: UUID,
    discountId: Option[UUID],
    title: Option[String],
    `type`: DiscountType,
    amount: BigDecimal,
    totalAmount: Option[BigDecimal],
    currency: Option[Currency],
  )

final case class ItemDiscountUpsertion(
    id: Option[UUID],
    discountId: Option[UUID],
    title: Option[String],
    `type`: DiscountType,
    amount: BigDecimal,
    totalAmount: Option[BigDecimal],
  )
