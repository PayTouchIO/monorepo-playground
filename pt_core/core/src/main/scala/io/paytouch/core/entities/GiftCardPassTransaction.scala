package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.GiftCardPassTransactionType
import io.paytouch.core.entities.enums.ExposedName

final case class GiftCardPassTransaction(
    id: UUID,
    total: MonetaryAmount,
    createdAt: ZonedDateTime,
    `type`: GiftCardPassTransactionType,
    pass: Option[GiftCardPass],
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.GiftCardPassTransaction
}
