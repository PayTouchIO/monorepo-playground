package io.paytouch.core.reports.entities

import io.paytouch.core.utils.EnumEntrySnake

final case class ReportFields[T](key: Option[String], values: T)

object ReportFields {

  def apply[T](key: EnumEntrySnake, values: T): ReportFields[T] = apply(key = Some(key.entryName), values = values)

}
