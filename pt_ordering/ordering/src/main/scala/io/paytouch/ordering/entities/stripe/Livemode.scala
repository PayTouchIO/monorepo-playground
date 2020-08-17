package io.paytouch.ordering.entities.stripe

import io.paytouch._

final case class Livemode(value: Boolean) extends Opaque[Boolean]
case object Livemode extends OpaqueCompanion[Boolean, Livemode]
