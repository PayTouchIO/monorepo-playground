package io.paytouch.ordering.clients.paytouch.core.entities

import io.paytouch._

final case class Country(code: Country.Code, name: Country.Name)
object Country {
  final case class Code(value: String) extends Opaque[String]
  case object Code extends OpaqueCompanion[String, Code]

  final case class Name(value: String) extends Opaque[String]
  case object Name extends OpaqueCompanion[String, Name]
}
