package io.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait MerchantSetupStatus extends EnumEntrySnake

case object MerchantSetupStatus extends Enum[MerchantSetupStatus] {

  case object Pending extends MerchantSetupStatus
  case object Completed extends MerchantSetupStatus
  case object Skipped extends MerchantSetupStatus

  val values = findValues
}
