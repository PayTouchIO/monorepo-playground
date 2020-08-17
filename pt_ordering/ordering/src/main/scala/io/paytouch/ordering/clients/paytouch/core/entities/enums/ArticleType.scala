package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._

import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed abstract class ArticleType extends EnumEntrySnake

case object ArticleType extends Enum[ArticleType] {
  case object GiftCard extends ArticleType
  case object Simple extends ArticleType
  case object Template extends ArticleType
  case object Variant extends ArticleType

  val values = findValues
}
