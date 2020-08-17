package io.paytouch.ordering.entities.enums

import enumeratum.Enum

sealed abstract class ExposedName extends EnumEntrySnake

case object ExposedName extends Enum[ExposedName] {
  case object Cart extends ExposedName
  case object GiftCard extends ExposedName
  case object GiftCardPass extends ExposedName
  case object GiftCardPassChargeFailure extends ExposedName
  case object IdsUsage extends ExposedName
  case object ImageUpload extends ExposedName
  case object Merchant extends ExposedName
  case object PaymentIntent extends ExposedName
  case object PaymentProcessorConfig extends ExposedName
  case object Store extends ExposedName
  case object WorldpaySubmitResponse extends ExposedName

  val values = findValues
}
