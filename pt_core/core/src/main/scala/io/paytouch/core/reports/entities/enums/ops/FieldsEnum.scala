package io.paytouch.core.reports.entities.enums.ops

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

trait FieldsEnum extends EnumEntrySnake with CustomisableField

trait Fields[A <: FieldsEnum] extends Enum[A] {
  def orderedValues = values.filterNot(_.toIgnore).sortBy(_.entryName)

  def orderedValuesWithExpandedFirst =
    alwaysExpanded.sortBy(_.entryName) ++ orderedValuesWithoutExpanded

  def orderedValuesWithoutExpanded =
    orderedValues.filterNot(alwaysExpanded.contains)

  def alwaysExpanded = Seq.empty[A]
}
