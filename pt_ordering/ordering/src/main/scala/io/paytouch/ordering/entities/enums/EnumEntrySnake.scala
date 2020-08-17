package io.paytouch.ordering.entities.enums

import enumeratum._

import io.paytouch._

trait EnumEntrySnake extends EnumEntry.Snakecase with SerializableProduct {
  def isEquivalent(e: EnumEntrySnake): Boolean =
    this.entryName == e.entryName
}
