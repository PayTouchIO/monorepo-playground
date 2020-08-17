package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.ChangeReason
import io.paytouch.core.entities.enums.ExposedName

final case class ProductPriceHistory(
    id: UUID,
    location: Location,
    timestamp: ZonedDateTime,
    prevPrice: MonetaryAmount,
    priceChange: MonetaryAmount,
    newPrice: MonetaryAmount,
    reason: ChangeReason,
    user: UserInfo,
    notes: Option[String],
  ) extends ExposedEntity {
  val classShortName = ExposedName.ProductPriceChange
}
