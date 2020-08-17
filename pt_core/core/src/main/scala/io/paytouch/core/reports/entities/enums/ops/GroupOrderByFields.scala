package io.paytouch.core.reports.entities.enums.ops

import io.paytouch.core.reports.entities.enums._

sealed trait GroupOrderByFields extends OrderByFieldsEnum {
  def columnName: String
}

sealed abstract class GroupOrderByFieldsHelper(val columnName: String) extends GroupOrderByFields
sealed abstract class GroupOrderByCopiedFieldsHelper(other: CustomisableField)
    extends CustomisableCopiedField(other)
       with GroupOrderByFields

case object GroupOrderByFields extends OrderByFields[GroupOrderByFields] {
  case object Id extends GroupOrderByFieldsHelper("id") {
    override val groupBy = Some(s"groups.$columnName")
  }

  case object Name extends GroupOrderByFieldsHelper("name") {
    override val groupBy = Some(s"groups.$columnName")
  }

  case object Profit extends GroupOrderByCopiedFieldsHelper(CustomerOrderByFields.Profit) with DescOrdering

  case object Spend extends GroupOrderByCopiedFieldsHelper(CustomerFields.Spend) with DescOrdering

  case object Visit extends GroupOrderByCopiedFieldsHelper(CustomerOrderByFields.Visit) with DescOrdering
  case object Margin extends GroupOrderByCopiedFieldsHelper(CustomerOrderByFields.Margin) with DescOrdering

  val values = findValues
}
