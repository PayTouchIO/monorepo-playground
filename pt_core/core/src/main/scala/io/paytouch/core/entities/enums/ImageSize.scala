package io.paytouch.core.entities.enums

import io.paytouch.core.utils.EnumEntrySnake

trait ImageSize extends EnumEntrySnake {
  val description = entryName
  def size: Option[Int]
  def cloudinaryFormatString: String
}
