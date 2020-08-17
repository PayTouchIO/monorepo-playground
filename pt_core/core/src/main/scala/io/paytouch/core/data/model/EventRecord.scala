package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.{ ExposedName, TrackableAction }
import io.paytouch.core.utils.UtcTime
import io.paytouch.core.json.JsonSupport.JValue

final case class EventRecord(
    id: UUID,
    merchantId: UUID,
    action: TrackableAction,
    `object`: ExposedName,
    data: Option[JValue],
    receivedAt: ZonedDateTime,
    createdAt: ZonedDateTime = UtcTime.now,
    updatedAt: ZonedDateTime = UtcTime.now,
  ) extends SlickMerchantRecord
