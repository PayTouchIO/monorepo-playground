package io.paytouch.core.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ArticleScope extends EnumEntrySnake

case object ArticleScope extends Enum[ArticleScope] {

  case object Product extends ArticleScope
  case object Part extends ArticleScope

  val values = findValues
}
