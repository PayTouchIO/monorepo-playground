package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.OrderStatus

final case class StatusTransition(
    id: UUID,
    status: OrderStatus,
    createdAt: Option[ZonedDateTime],
  )
