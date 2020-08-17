package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.QuantityChangeReason
import io.paytouch.core.entities.enums.ExposedName

final case class ProductQuantityHistory(
    id: UUID,
    location: Location,
    timestamp: ZonedDateTime,
    prevQuantity: BigDecimal,
    newQuantity: BigDecimal,
    newStockValue: MonetaryAmount,
    reason: QuantityChangeReason,
    user: Option[UserInfo],
    notes: Option[String],
  ) extends ExposedEntity {
  val classShortName = ExposedName.ProductQuantityChange
}
