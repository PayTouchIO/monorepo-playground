package io.paytouch.core.conversions

import java.io.File
import java.util.UUID

import io.paytouch.core.data.model.enums.ImportType
import io.paytouch.core.data.model.{ ImportRecord, ImportUpdate }
import io.paytouch.core.entities.{ UserContext, Import => ImportEntity }

trait ImportConversions extends EntityConversion[ImportRecord, ImportEntity] {

  def fromRecordToEntity(record: ImportRecord)(implicit user: UserContext): ImportEntity =
    ImportEntity(
      id = record.id,
      `type` = record.`type`,
      locationIds = record.locationIds,
      validationStatus = record.validationStatus,
      importStatus = record.importStatus,
      validationErrors = record.validationErrors,
      importSummary = record.importSummary,
      deleteExisting = record.deleteExisting,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def toImportUpdate(
      id: UUID,
      file: File,
      locationIds: Seq[UUID],
      deleteExisting: Option[Boolean],
      `type`: ImportType,
    )(implicit
      user: UserContext,
    ) =
    ImportUpdate(
      id = Some(id),
      `type` = Some(`type`),
      merchantId = Some(user.merchantId),
      locationIds = Some(locationIds),
      filename = Some(file.getPath),
      deleteExisting = deleteExisting,
    )
}
