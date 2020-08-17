package io.paytouch.core.reports.entities.enums

import enumeratum.Enum
import io.paytouch.core.reports.entities.enums.ops.{ Fields, FieldsEnum }
import io.paytouch.core.utils.EnumEntryAllUpperCase

trait OrderByFieldsEnum extends FieldsEnum {
  val ordering: Ordering = Ordering.Asc

  sealed trait Ordering extends EnumEntryAllUpperCase

  case object Ordering extends Enum[Ordering] {
    case object Asc extends Ordering
    case object Desc extends Ordering

    val values = findValues
  }
}

trait OrderByFields[A <: OrderByFieldsEnum] extends Fields[A]
