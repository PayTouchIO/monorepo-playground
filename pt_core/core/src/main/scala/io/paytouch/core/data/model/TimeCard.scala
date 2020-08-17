package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.utils.UtcTime

final case class TimeCardRecord(
    id: UUID,
    merchantId: UUID,
    userId: UUID,
    locationId: UUID,
    shiftId: Option[UUID],
    deltaMins: Int,
    totalMins: Option[Int],
    regularMins: Option[Int],
    overtimeMins: Option[Int],
    unpaidBreakMins: Option[Int],
    notes: Option[String],
    startAt: Option[ZonedDateTime],
    endAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class TimeCardUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    userId: Option[UUID],
    locationId: Option[UUID],
    shiftId: Option[UUID],
    deltaMins: Option[Int],
    totalMins: Option[Int],
    regularMins: Option[Int],
    overtimeMins: Option[Int],
    unpaidBreakMins: Option[Int],
    notes: Option[String],
    startAt: Option[ZonedDateTime],
    endAt: Option[ZonedDateTime],
  ) extends SlickMerchantUpdate[TimeCardRecord] {

  def toRecord: TimeCardRecord = {
    require(merchantId.isDefined, s"Impossible to convert TimeCardUpdate without a merchant id. [$this]")
    require(userId.isDefined, s"Impossible to convert TimeCardUpdate without a user id. [$this]")
    require(locationId.isDefined, s"Impossible to convert TimeCardUpdate without a location id. [$this]")
    TimeCardRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      userId = userId.get,
      locationId = locationId.get,
      shiftId = shiftId,
      deltaMins = deltaMins.getOrElse(0),
      totalMins = totalMins,
      regularMins = regularMins,
      overtimeMins = overtimeMins,
      unpaidBreakMins = unpaidBreakMins,
      notes = notes,
      startAt = startAt,
      endAt = endAt,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: TimeCardRecord): TimeCardRecord =
    TimeCardRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      userId = userId.getOrElse(record.userId),
      locationId = locationId.getOrElse(record.locationId),
      shiftId = shiftId.orElse(record.shiftId),
      deltaMins = deltaMins.getOrElse(record.deltaMins),
      totalMins = totalMins.orElse(record.totalMins),
      regularMins = regularMins.orElse(record.regularMins),
      overtimeMins = overtimeMins.orElse(record.overtimeMins),
      unpaidBreakMins = unpaidBreakMins.orElse(record.unpaidBreakMins),
      notes = notes.orElse(record.notes),
      startAt = startAt.orElse(record.startAt),
      endAt = endAt.orElse(record.endAt),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object TimeCardUpdate {
  def empty =
    TimeCardUpdate(
      id = None,
      merchantId = None,
      userId = None,
      locationId = None,
      shiftId = None,
      deltaMins = None,
      totalMins = None,
      regularMins = None,
      overtimeMins = None,
      unpaidBreakMins = None,
      notes = None,
      startAt = None,
      endAt = None,
    )
}

final case class TimeCardTotals(
    deltaMins: Int,
    totalMins: Int,
    regularMins: Int,
    overtimeMins: Int,
  )

object TimeCardTotals {
  def zero = TimeCardTotals(0, 0, 0, 0)
}
