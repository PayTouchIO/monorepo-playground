package io.paytouch.core.reports.data.model.enums

import enumeratum._
import io.paytouch.core.utils.EnumEntrySnake

sealed trait ExportStatus extends EnumEntrySnake

case object ExportStatus extends Enum[ExportStatus] {

  case object NotStarted extends ExportStatus
  case object Processing extends ExportStatus
  case object Uploading extends ExportStatus
  case object Completed extends ExportStatus
  case object Failed extends ExportStatus

  val values = findValues
}
