package io.paytouch.core.reports.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.ExposedEntity
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.reports.data.model.enums.ExportStatus

final case class Export(
    id: UUID,
    `type`: String,
    status: ExportStatus,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {

  val classShortName = ExposedName.Export
}

final case class ExportDownload(url: String) extends ExposedEntity {
  val classShortName = ExposedName.ExportDownload
}
