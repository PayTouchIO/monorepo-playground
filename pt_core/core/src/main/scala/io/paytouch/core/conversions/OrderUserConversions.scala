package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.OrderUserUpdate
import io.paytouch.core.entities.UserContext

trait OrderUserConversions {

  def toUpdate(orderId: UUID, userId: UUID)(implicit user: UserContext): OrderUserUpdate =
    OrderUserUpdate(id = None, merchantId = Some(user.merchantId), orderId = Some(orderId), userId = Some(userId))
}
