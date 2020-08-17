package io.paytouch.ordering.entities

import io.paytouch.ordering.entities.enums.PaymentMethodType

final case class PaymentMethod(`type`: PaymentMethodType, active: Boolean) {
  val inactive: Boolean = !active
}
