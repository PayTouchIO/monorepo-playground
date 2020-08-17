package io.paytouch.core.reports.errors

import java.util.UUID

import io.paytouch.core.errors.NonAccessibleIds

object NonAccessibleExportIds {
  def apply(ids: Seq[UUID]): NonAccessibleIds =
    NonAccessibleIds(message = "Image upload ids not accessible", code = "NonAccessibleExportIds", values = ids)
}
