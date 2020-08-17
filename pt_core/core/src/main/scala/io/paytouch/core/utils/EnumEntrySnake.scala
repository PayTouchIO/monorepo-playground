package io.paytouch.core.utils

import enumeratum._

import io.paytouch._

trait EnumEntrySnake extends EnumEntry.Snakecase with SerializableProduct {
  def isEquivalent(e: EnumEntrySnake): Boolean =
    this.entryName == e.entryName
}

trait EnumEntryFirstLowercase extends EnumEntry with FirstLowercase

trait FirstLowercase extends EnumEntry {
  abstract override def entryName: String = {
    val s = super.entryName
    s"${s.charAt(0).toLower}${s.substring(1)}"
  }
}

trait EnumEntryAllUpperCase extends EnumEntry {
  abstract override def entryName: String = super.entryName.toUpperCase
}
