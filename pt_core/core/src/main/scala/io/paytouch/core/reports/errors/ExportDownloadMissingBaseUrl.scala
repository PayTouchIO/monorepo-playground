package io.paytouch.core.reports.errors

import io.paytouch.core.errors._
import io.paytouch.core.reports.data.model.ExportRecord

object ExportDownloadMissingBaseUrl {
  def apply(export: ExportRecord): DataError =
    ArbitraryDataError(
      message = s"Cannot download export as base url is missing. Is the export completed? (status is ${export.status})",
      code = "ExportDownloadMissingBaseUrl",
      values = Seq(export.id),
    )
}
