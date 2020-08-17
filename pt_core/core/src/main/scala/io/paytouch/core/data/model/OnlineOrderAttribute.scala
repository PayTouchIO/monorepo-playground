package io.paytouch.core.data.model

import cats.implicits._
import java.time.{ LocalTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.data.model.enums.{ AcceptanceStatus, CancellationStatus }
import io.paytouch.core.entities.{ ResettableInt, ResettableLocalTime, ResettableString, ResettableZonedDateTime }

final case class OnlineOrderAttributeRecord(
    id: UUID,
    merchantId: UUID,
    acceptanceStatus: AcceptanceStatus,
    rejectionReason: Option[String],
    prepareByTime: Option[LocalTime],
    prepareByDateTime: Option[ZonedDateTime],
    estimatedPrepTimeInMins: Option[Int],
    acceptedAt: Option[ZonedDateTime],
    rejectedAt: Option[ZonedDateTime],
    estimatedReadyAt: Option[ZonedDateTime],
    estimatedDeliveredAt: Option[ZonedDateTime],
    cancellationStatus: Option[CancellationStatus],
    cancellationReason: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord {
  def deriveUpdateFromPreviousState(other: OnlineOrderAttributeRecord): OnlineOrderAttributeUpdate =
    OnlineOrderAttributeUpdate(
      id = id.some,
      merchantId = if (merchantId != other.merchantId) merchantId.some else None, //   Option[UUID],
      acceptanceStatus =
        if (acceptanceStatus != other.acceptanceStatus) acceptanceStatus.some else None, //   Option[AcceptanceStatus],
      rejectionReason = if (rejectionReason != other.rejectionReason) rejectionReason else None, //   ResettableString,
      prepareByTime = if (prepareByTime != other.prepareByTime) prepareByTime else None, //   ResettableLocalTime,
      prepareByDateTime =
        if (prepareByDateTime != other.prepareByDateTime) prepareByDateTime else None, //   ResettableZonedDateTime,
      estimatedPrepTimeInMins =
        if (estimatedPrepTimeInMins != other.estimatedPrepTimeInMins) estimatedPrepTimeInMins
        else None, //   ResettableInt,
      acceptedAt = if (acceptedAt != other.acceptedAt) acceptedAt else None, //   Option[ZonedDateTime],
      rejectedAt = if (rejectedAt != other.rejectedAt) rejectedAt else None, //   Option[ZonedDateTime],
      cancellationStatus =
        if (cancellationStatus != other.cancellationStatus) cancellationStatus
        else None, //   Option[CancellationStatus],
      cancellationReason =
        if (cancellationReason != other.cancellationReason) cancellationReason else None, //   Option[String],
      estimatedReadyAt =
        if (estimatedReadyAt != other.estimatedReadyAt) estimatedReadyAt else None, //   ResettableZonedDateTime,
      estimatedDeliveredAt =
        if (estimatedDeliveredAt != other.estimatedDeliveredAt) estimatedDeliveredAt
        else None, //   ResettableZonedDateTime,
    )
}

case class OnlineOrderAttributeUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    acceptanceStatus: Option[AcceptanceStatus],
    rejectionReason: ResettableString,
    prepareByTime: ResettableLocalTime,
    prepareByDateTime: ResettableZonedDateTime,
    estimatedPrepTimeInMins: ResettableInt,
    acceptedAt: Option[ZonedDateTime],
    rejectedAt: Option[ZonedDateTime],
    cancellationStatus: Option[CancellationStatus],
    cancellationReason: Option[String],
    estimatedReadyAt: ResettableZonedDateTime,
    estimatedDeliveredAt: ResettableZonedDateTime,
  ) extends SlickMerchantUpdate[OnlineOrderAttributeRecord] {

  def updateRecord(record: OnlineOrderAttributeRecord): OnlineOrderAttributeRecord =
    OnlineOrderAttributeRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      acceptanceStatus = acceptanceStatus.getOrElse(record.acceptanceStatus),
      rejectionReason = rejectionReason.getOrElse(record.rejectionReason),
      prepareByTime = prepareByTime.getOrElse(record.prepareByTime),
      prepareByDateTime = prepareByDateTime.getOrElse(record.prepareByDateTime),
      estimatedPrepTimeInMins = estimatedPrepTimeInMins.getOrElse(record.estimatedPrepTimeInMins),
      acceptedAt = acceptedAt.orElse(record.acceptedAt),
      rejectedAt = rejectedAt.orElse(record.rejectedAt),
      estimatedReadyAt = estimatedReadyAt.getOrElse(record.estimatedReadyAt),
      estimatedDeliveredAt = estimatedDeliveredAt.getOrElse(record.estimatedDeliveredAt),
      cancellationStatus = cancellationStatus.orElse(record.cancellationStatus),
      cancellationReason = cancellationReason.orElse(record.cancellationReason),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: OnlineOrderAttributeRecord = {
    require(merchantId.isDefined, s"Impossible to convert OnlineOrderAttributeUpdate without a merchant id. [$this]")
    OnlineOrderAttributeRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      acceptanceStatus = acceptanceStatus.getOrElse(AcceptanceStatus.Pending),
      rejectionReason = rejectionReason,
      prepareByTime = prepareByTime,
      prepareByDateTime = prepareByDateTime,
      estimatedPrepTimeInMins = estimatedPrepTimeInMins,
      acceptedAt = acceptedAt,
      rejectedAt = rejectedAt,
      estimatedReadyAt = estimatedReadyAt,
      estimatedDeliveredAt = estimatedDeliveredAt,
      cancellationStatus = cancellationStatus,
      cancellationReason = cancellationReason,
      createdAt = now,
      updatedAt = now,
    )
  }
}

object OnlineOrderAttributeUpdate {
  def empty =
    OnlineOrderAttributeUpdate(
      id = None,
      merchantId = None,
      acceptanceStatus = None,
      rejectionReason = None,
      prepareByTime = None,
      prepareByDateTime = None,
      estimatedPrepTimeInMins = None,
      acceptedAt = None,
      rejectedAt = None,
      estimatedReadyAt = None,
      estimatedDeliveredAt = None,
      cancellationStatus = None,
      cancellationReason = None,
    )
}
