package io.paytouch.ordering
package entities

import java.util.UUID

final case class Default[T](optT: Option[T], default: T) {

  def getOrDefault: T =
    optT.getOrElse(default)

}

object Default {

  implicit def toOption[T](default: Default[T]): Option[T] = default.optT

  implicit def fromOptIdToDefaultUUID(optId: Option[UUID]): Default[UUID] =
    Default(optId, UUID.randomUUID)

  implicit def fromIdToDefaultUUID(id: UUID): Default[UUID] =
    fromOptIdToDefaultUUID(Some(id))
}
