package io.paytouch.ordering.clients.paytouch.core.entities

import io.paytouch._

final case class State(
    name: Option[State.Name],
    code: State.Code,
    country: Option[Country],
  )

object State {
  final case class Code(value: String) extends Opaque[String]
  case object Code extends OpaqueCompanion[String, Code]

  final case class Name(value: String) extends Opaque[String]
  case object Name extends OpaqueCompanion[String, Name]
}
