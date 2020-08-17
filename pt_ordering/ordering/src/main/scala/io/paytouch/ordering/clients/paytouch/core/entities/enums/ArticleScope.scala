package io.paytouch.ordering.clients.paytouch.core.entities.enums

import enumeratum._
import io.paytouch.ordering.entities.enums.EnumEntrySnake

sealed trait ArticleScope extends EnumEntrySnake

case object ArticleScope extends Enum[ArticleScope] {

  case object Product extends ArticleScope
  case object Part extends ArticleScope

  val values = findValues
}
