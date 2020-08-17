package io.paytouch.core.reports.conversions

import io.paytouch.core.conversions.EntityConversion
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.data.model.enums.ExportStatus
import io.paytouch.core.reports.data.model.{ ExportRecord, ExportUpdate }
import io.paytouch.core.reports.entities.Export

trait ExportConversions extends EntityConversion[ExportRecord, Export] {

  def fromRecordToEntity(record: ExportRecord)(implicit user: UserContext): Export =
    Export(
      id = record.id,
      `type` = record.`type`,
      status = record.status,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  protected def toExportUpdate(exportType: String)(implicit user: UserContext) =
    ExportUpdate(
      id = None,
      `type` = Some(exportType),
      merchantId = Some(user.merchantId),
      status = Some(ExportStatus.NotStarted),
      baseUrl = None,
    )
}
