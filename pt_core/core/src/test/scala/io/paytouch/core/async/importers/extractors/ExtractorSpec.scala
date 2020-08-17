package io.paytouch.core.async.importers.extractors

import java.util.UUID

import io.paytouch.core.async.importers.parsers.EnrichedDataRow
import io.paytouch.core.data.daos.{ ConfiguredTestDatabase, DaoSpec }
import io.paytouch.core.data.model.ImportRecord
import io.paytouch.core.data.model.enums.{ ImportStatus, ImportType }
import io.paytouch.core.utils.{ UtcTime, ValidatedHelpers, FixtureDaoFactory => Factory }
import io.paytouch.utils.TestExecutionContext
import org.specs2.specification.Scope

abstract class ExtractorSpec extends DaoSpec { self =>
  abstract class ExtractorSpecContext
      extends Scope
         with TestExecutionContext
         with ConfiguredTestDatabase
         with ValidatedHelpers {

    val daos = self.daos

    val merchant = Factory.merchant.create
    val defaultMenuCatalog = Factory.defaultMenuCatalog(merchant).create
    val location = Factory.location(merchant).create
    val locations = Seq(location)

    val importRecord = ImportRecord(
      id = UUID.randomUUID,
      `type` = ImportType.Product,
      merchantId = merchant.id,
      locationIds = Seq(location.id),
      filename = "a/file/name",
      validationStatus = ImportStatus.NotStarted,
      importStatus = ImportStatus.NotStarted,
      validationErrors = None,
      importSummary = None,
      deleteExisting = false,
      createdAt = UtcTime.now,
      updatedAt = UtcTime.now,
    )

    def buildDataWithLineCount(data: List[Map[String, List[String]]]): Seq[EnrichedDataRow] =
      data.zipWithIndex.map { case (rows, idx) => EnrichedDataRow(idx + 1, rows) }
  }
}
