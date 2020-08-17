package io.paytouch.core.reports.errors

import java.util.UUID

import io.paytouch.core.errors.InvalidIds

object InvalidExportIds {
  def apply(ids: Seq[UUID]): InvalidIds =
    InvalidIds(message = "Export ids not valid", code = "InvalidExportIds", values = ids)
}
