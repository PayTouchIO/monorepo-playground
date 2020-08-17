package io.paytouch.core.entities

import cats.implicits._

import io.scalaland.chimney.dsl._

final case class State(
    name: String,
    code: String,
    country: Option[Country],
  ) {
  final def toAddressState: AddressState =
    this.transformInto[AddressState]
}

final case class AddressState(
    name: Option[String],
    code: String,
    country: Option[Country],
  ) {
  final def toState(fallbackName: => String): State =
    this
      .into[State]
      .withFieldConst(_.name, name.getOrElse(fallbackName))
      .transform
}
