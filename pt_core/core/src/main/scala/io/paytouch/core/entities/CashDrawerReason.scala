package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.enums.CashDrawerActivityType
import io.paytouch.core.entities.enums.ExposedName

final case class CashDrawerReason(
    id: UUID,
    reasonText: String,
    `type`: CashDrawerActivityType,
    position: Int,
  ) extends ExposedEntity {
  val classShortName = ExposedName.CashDrawerReason
}
