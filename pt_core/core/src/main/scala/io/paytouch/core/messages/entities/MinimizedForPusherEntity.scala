package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{ CashDrawer, CashDrawerActivity, ExposedEntity, Product, Stock, TipsAssignment }

/**
  * Pusher has a message size limit, if we send an entity that is too big it will fail badly.
  * For large entities we need to just send an id and force the listening clients to refetch data from core.
  * For small/medium entities that are updated frequently AND do not grow indefinitely,
  * we can still send them as-they-are, but this type-class mechanism enforces us to make such analysis.
  *
  * @tparam Entity
  */
trait MinimizedForPusherEntity[Entity] {
  type Minimized <: ExposedEntity
  def toMinimized(entity: Entity): Minimized
}

final case class IdOnlyEntity(id: UUID, classShortName: ExposedName) extends ExposedEntity

object MinimizedForPusherEntity {
  def apply[A](implicit min: MinimizedForPusherEntity[A]): MinimizedForPusherEntity[A] = min

  def instance[A, M <: ExposedEntity](func: A => M): MinimizedForPusherEntity[A] =
    new MinimizedForPusherEntity[A] {
      type Minimized = M
      def toMinimized(entity: A): M = func(entity)
    }

  def alreadyMinimized[A <: ExposedEntity]: MinimizedForPusherEntity[A] = instance[A, A](identity)
  def toIdOnly[A <: ExposedEntity](toId: A => UUID) =
    instance[A, IdOnlyEntity](e => IdOnlyEntity(toId(e), e.classShortName))

  implicit val minimizedStock = alreadyMinimized[Stock]
  implicit val minimizedProduct = toIdOnly[Product](_.id)
  implicit val minimizedCashDrawer = toIdOnly[CashDrawer](_.id)
  implicit val minimizedCashDrawerActivity = toIdOnly[CashDrawerActivity](_.id)
  implicit val minimizedTipAssignment = toIdOnly[TipsAssignment](_.id)

}
