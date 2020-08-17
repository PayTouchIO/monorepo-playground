package io.paytouch.core.data.model.enums

import enumeratum._

import io.paytouch.core.entities._
import io.paytouch.core.utils.EnumEntrySnake

sealed abstract class SetupType(val features: MerchantFeatures) extends EnumEntrySnake

case object SetupType extends Enum[SetupType] {
  case object Dash
      extends SetupType(
        MerchantFeatures
          .allTrue
          .copy(
            pos = MerchantFeature(enabled = false),
            inventory = MerchantFeature(enabled = false),
            tables = MerchantFeature(enabled = false),
            coupons = MerchantFeature(enabled = false),
            loyalty = MerchantFeature(enabled = false),
            engagement = MerchantFeature(enabled = false),
          ),
      )

  case object Paytouch extends SetupType(MerchantFeatures.allTrue)

  val values = findValues
}
