package io.paytouch.core.entities

import java.util.UUID

trait CreationEntity[E <: ExposedEntity, T <: UpdateEntity[E]] {
  def asUpdate: T
}

trait CreationEntityWithRelIds[E <: ExposedEntity, T <: UpdateEntityWithRelIds[E]] extends CreationEntity[E, T] {
  def relId1: UUID
  def relId2: UUID
}

trait UpdateEntity[E <: ExposedEntity]

trait UpdateEntityWithRelIds[E <: ExposedEntity] extends UpdateEntity[E] {
  def relId1: UUID
  def relId2: UUID
}
