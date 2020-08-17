package io.paytouch.core.conversions

import io.paytouch.core.data.model.EventRecord
import io.paytouch.core.entities.{ UserContext, Event => EventEntity }

trait EventConversions extends EntityConversion[EventRecord, EventEntity] {

  def fromRecordToEntity(record: EventRecord)(implicit user: UserContext): EventEntity =
    EventEntity(
      id = record.id,
      action = record.action,
      `object` = record.`object`,
      data = record.data,
      receivedAt = record.receivedAt,
    )

}
