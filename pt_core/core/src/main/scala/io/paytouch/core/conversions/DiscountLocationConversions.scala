package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ DiscountLocationRecord, DiscountLocationUpdate }
import io.paytouch.core.entities.UserContext

trait DiscountLocationConversions {

  def toDiscountLocationUpdate(discountId: UUID, locationId: UUID)(implicit user: UserContext): DiscountLocationUpdate =
    DiscountLocationUpdate(
      id = Some(UUID.randomUUID),
      merchantId = Some(user.merchantId),
      discountId = Some(discountId),
      locationId = Some(locationId),
      active = None,
    )

  def toDiscountLocationUpdate(record: DiscountLocationRecord)(implicit user: UserContext): DiscountLocationUpdate =
    DiscountLocationUpdate(
      id = Some(record.id),
      merchantId = Some(user.merchantId),
      discountId = Some(record.discountId),
      locationId = Some(record.locationId),
      active = Some(record.active),
    )
}
