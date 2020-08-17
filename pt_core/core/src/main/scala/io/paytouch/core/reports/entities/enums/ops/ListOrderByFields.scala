package io.paytouch.core.reports.entities.enums.ops

import io.paytouch.core.reports.entities.enums.{ OrderByFields, OrderByFieldsEnum }

trait ListOrderByFieldsEnum extends OrderByFieldsEnum

trait ListOrderByFields[A <: ListOrderByFieldsEnum] extends OrderByFields[A] {

  def defaultOrdering: Seq[A]
}
