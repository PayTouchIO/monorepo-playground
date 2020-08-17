package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.CustomerGroupUpdate
import io.paytouch.core.entities.UserContext

trait CustomerGroupConversions {

  def fromCustomerIdsToCustomerGroupUpdates(
      groupId: Option[UUID],
      customerIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Seq[CustomerGroupUpdate] =
    customerIds.map(fromCustomerIdToCustomerGroupUpdate(groupId, _))

  def fromCustomerIdToCustomerGroupUpdate(
      groupId: Option[UUID],
      customerId: UUID,
    )(implicit
      user: UserContext,
    ): CustomerGroupUpdate =
    CustomerGroupUpdate(id = None, merchantId = Some(user.merchantId), customerId = Some(customerId), groupId = groupId)
}
