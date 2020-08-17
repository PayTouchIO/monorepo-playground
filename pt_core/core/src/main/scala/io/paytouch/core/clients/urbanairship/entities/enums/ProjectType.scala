package io.paytouch.core.clients.urbanairship.entities.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntryFirstLowercase

sealed trait ProjectType extends EnumEntryFirstLowercase

case object ProjectType extends Enum[ProjectType] {

  case object Loyalty extends ProjectType
  case object Coupon extends ProjectType
  case object GiftCard extends ProjectType
  case object MemberCard extends ProjectType
  case object EventTicket extends ProjectType
  case object BoardingPass extends ProjectType
  case object Generic extends ProjectType

  val values = findValues
}
