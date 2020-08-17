package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ ImportStatus, ImportType }
import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.json.JsonSupport.JValue

final case class Import(
    id: UUID,
    `type`: ImportType,
    locationIds: Seq[UUID],
    validationStatus: ImportStatus,
    importStatus: ImportStatus,
    validationErrors: Option[JValue],
    importSummary: Option[JValue],
    deleteExisting: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {

  val classShortName = ExposedName.Import
}
