package io.paytouch.core.conversions

import io.paytouch.core.data.model.{ SlickItemLocationRecord, SlickToggleableRecord }
import io.paytouch.core.entities.ItemLocation

trait ItemLocationConversions[T <: SlickItemLocationRecord with SlickToggleableRecord] {
  def fromItemLocationToEntity(itemLocationRecord: T): ItemLocation =
    ItemLocation(active = itemLocationRecord.active)
}
