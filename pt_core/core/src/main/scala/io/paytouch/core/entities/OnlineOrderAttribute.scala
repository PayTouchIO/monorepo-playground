package io.paytouch.core.entities

import java.time.{ LocalTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.data.model.enums.{ AcceptanceStatus, CancellationStatus }

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
    cancellationStatus: Option[CancellationStatus],
    cancellationReason: Option[String],
  )

final case class OnlineOrderAttributeUpsertion(
    id: UUID,
    email: ResettableString,
    firstName: ResettableString,
    lastName: ResettableString,
    phoneNumber: ResettableString,
    prepareByTime: ResettableLocalTime,
    prepareByDateTime: ResettableZonedDateTime,
    estimatedPrepTimeInMins: ResettableInt,
    acceptanceStatus: Option[AcceptanceStatus],
    cancellationStatus: Option[CancellationStatus],
    cancellationReason: Option[String],
  )
