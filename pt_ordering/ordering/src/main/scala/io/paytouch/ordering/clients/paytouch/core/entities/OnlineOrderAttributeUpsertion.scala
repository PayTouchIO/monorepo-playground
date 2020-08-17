package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID
import java.time._

import io.paytouch.ordering.entities.{ ResettableLocalTime, ResettableString }
import io.paytouch.ordering.clients.paytouch.core.entities.enums.AcceptanceStatus
import java.time.LocalTime

final case class OnlineOrderAttribute(
    id: UUID,
    acceptanceStatus: AcceptanceStatus,
    rejectionReason: Option[String],
    prepareByTime: Option[LocalTime],
    prepareByDateTime: Option[ZonedDateTime],
    estimatedPrepTimeInMins: Option[Int],
    rejectedAt: Option[ZonedDateTime],
    acceptedAt: Option[ZonedDateTime],
    estimatedReadyAt: Option[ZonedDateTime],
    estimatedDeliveredAt: Option[ZonedDateTime],
    cancellationReason: Option[String],
  )

final case class OnlineOrderAttributeUpsertion(
    id: UUID = UUID.randomUUID,
    email: ResettableString,
    firstName: ResettableString,
    lastName: ResettableString,
    phoneNumber: ResettableString,
    prepareByTime: ResettableLocalTime,
    acceptanceStatus: AcceptanceStatus,
  )
