package io.paytouch.core.reports.entities

import io.paytouch.core.utils.EnumEntrySnake

final case class ReportCount(key: Option[String], count: Int)

object ReportCount {

  def apply(key: EnumEntrySnake, count: Int): ReportCount = apply(key = Some(key.entryName), count = count)

}
