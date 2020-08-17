package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ ModifierSetLocationRecord, ModifierSetLocationUpdate }
import io.paytouch.core.entities.UserContext

trait ModifierSetLocationConversions {

  def toModifierSetLocationUpdates(
      modifierSetId: UUID,
      locationIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Seq[ModifierSetLocationUpdate] =
    locationIds.map(toModifierSetLocationUpdate(modifierSetId, _))

  def toModifierSetLocationUpdate(
      modifierSetId: UUID,
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): ModifierSetLocationUpdate =
    ModifierSetLocationUpdate(
      id = None,
      merchantId = Some(user.merchantId),
      modifierSetId = Some(modifierSetId),
      locationId = Some(locationId),
      active = None,
    )

  def toModifierSetLocationUpdate(
      record: ModifierSetLocationRecord,
    )(implicit
      user: UserContext,
    ): ModifierSetLocationUpdate =
    ModifierSetLocationUpdate(
      id = Some(record.id),
      merchantId = Some(user.merchantId),
      modifierSetId = Some(record.modifierSetId),
      locationId = Some(record.locationId),
      active = Some(record.active),
    )
}
