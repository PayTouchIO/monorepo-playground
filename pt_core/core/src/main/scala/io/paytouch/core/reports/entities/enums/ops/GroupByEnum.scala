package io.paytouch.core.reports.entities.enums.ops

import io.paytouch.core.utils.EnumEntrySnake

trait GroupByEnum extends EnumEntrySnake with CustomisableField {
  override def aggregatedSelector(op: String, table: String) = s"$table.$columnName"
}
