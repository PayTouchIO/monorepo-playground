package io.paytouch.core.reports.async.exporters

import java.util.UUID

import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.entities.enums.CsvExports
import io.paytouch.core.reports.queries.CsvExport

final case class CsvMsg[T <: CsvExports](
    exportId: UUID,
    csvExportType: CsvExports,
    filename: String,
    user: UserContext,
    cvsExport: CsvExport[T],
  ) { self =>
  def buildFilename() = cvsExport.buildFilename(self)
}
