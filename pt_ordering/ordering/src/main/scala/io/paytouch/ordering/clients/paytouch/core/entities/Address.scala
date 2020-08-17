package io.paytouch.ordering.clients.paytouch.core.entities

import java.net.URLEncoder

import io.paytouch._

final case class Address(
    line1: Option[String],
    line2: Option[String],
    city: Option[String],
    state: Option[String],
    country: Option[String],
    stateData: Option[State],
    postalCode: Option[String],
  ) {
  def toOrderingAddress: io.paytouch.ordering.entities.Address = {
    import io.scalaland.chimney.dsl._

    this.transformInto[io.paytouch.ordering.entities.Address]
  }
}
