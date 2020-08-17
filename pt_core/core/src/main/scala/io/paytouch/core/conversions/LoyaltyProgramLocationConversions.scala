package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.LoyaltyProgramLocationUpdate
import io.paytouch.core.entities.UserContext

trait LoyaltyProgramLocationConversions {

  def toLoyaltyProgramLocationUpdates(itemId: UUID, locationIds: Seq[UUID])(implicit user: UserContext) =
    locationIds.map(toLoyaltyProgramLocationUpdate(itemId, _))

  def toLoyaltyProgramLocationUpdate(
      itemId: UUID,
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): LoyaltyProgramLocationUpdate =
    LoyaltyProgramLocationUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      loyaltyProgramId = Some(itemId),
      locationId = Some(locationId),
    )
}
