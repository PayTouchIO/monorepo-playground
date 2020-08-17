package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{ ExposedEntity, UserContext }

final case class EntitySynced[E](eventName: String, payload: EntityPayload[E]) extends PtNotifierMsg[E]

object EntitySynced {

  def apply[E <: ExposedEntity](entity: E, locationId: UUID)(implicit user: UserContext): EntitySynced[E] =
    EntitySynced(entity.classShortName, entity, locationId)

  def apply[E](
      entityName: ExposedName,
      entity: E,
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): EntitySynced[E] = {
    val eventName = "entity_synced"
    val payload = EntityPayload[E](entityName, entity, user.merchantId, Some(locationId), user.pusherSocketId)
    EntitySynced(eventName, payload)
  }
}
