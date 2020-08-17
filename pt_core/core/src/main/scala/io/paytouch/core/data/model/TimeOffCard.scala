package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.TimeOffType

final case class TimeOffCardRecord(
    id: UUID,
    merchantId: UUID,
    userId: UUID,
    paid: Boolean,
    `type`: Option[TimeOffType],
    notes: Option[String],
    startAt: Option[ZonedDateTime],
    endAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class TimeOffCardUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    userId: Option[UUID],
    paid: Option[Boolean],
    `type`: Option[TimeOffType],
    notes: Option[String],
    startAt: Option[ZonedDateTime],
    endAt: Option[ZonedDateTime],
  ) extends SlickMerchantUpdate[TimeOffCardRecord] {

  def toRecord: TimeOffCardRecord = {
    require(merchantId.isDefined, s"Impossible to convert TimeOffCardUpdate without a merchant id. [$this]")
    require(userId.isDefined, s"Impossible to convert TimeOffCardUpdate without a user id. [$this]")
    TimeOffCardRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      userId = userId.get,
      paid = paid.getOrElse(false),
      `type` = `type`,
      notes = notes,
      startAt = startAt,
      endAt = endAt,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: TimeOffCardRecord): TimeOffCardRecord =
    TimeOffCardRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      userId = userId.getOrElse(record.userId),
      paid = paid.getOrElse(record.paid),
      `type` = `type`.orElse(record.`type`),
      notes = notes.orElse(record.notes),
      startAt = startAt.orElse(record.startAt),
      endAt = endAt.orElse(record.endAt),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
