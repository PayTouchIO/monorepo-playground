package io.paytouch.core.async.importers.extractors

import java.util.UUID

import io.paytouch.core.async.importers.parsers.EnrichedDataRow
import io.paytouch.core.async.importers.{ Keys, UpdatesWithCount }
import io.paytouch.core.data.model._
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait SupplierExtractor extends Extractor {

  val supplierDao = daos.supplierDao

  def extractSuppliers(
      data: Seq[EnrichedDataRow],
    )(implicit
      importRecord: ImportRecord,
    ): Future[Extraction[SupplierUpdate]] = {
    logExtraction("suppliers")
    val extractions = data.map(extractSuppliersPerRow)
    enrichSuppliers(MultipleExtraction.sequence(extractions))
  }

  private def extractSuppliersPerRow(
      row: EnrichedDataRow,
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[Seq[SupplierUpdate]] = {
    val names = row.getOrElse(Keys.Supplier, Seq.empty).filter(_.trim.nonEmpty)
    val suppliers = names.map(n => toSupplierUpdate(name = n))
    MultipleExtraction.success(suppliers)
  }

  protected def enrichSuppliers(
      extractedSuppliers: ErrorsOr[Seq[SupplierUpdate]],
    )(implicit
      importRecord: ImportRecord,
    ): Future[Extraction[SupplierUpdate]] = {
    val names = extractedSuppliers.getOrElse(Seq.empty).flatMap(_.name)
    for {
      existingSuppliers <- supplierDao.findByNamesAndMerchantId(names, importRecord.merchantId)
    } yield extractedSuppliers.map { extracted =>
      val extractedWithIds = extracted.filterNot(_.name.contains("")).distinctBy(_.name).map { supplier =>
        val existingId = existingSuppliers.find(c => supplier.name.contains(c.name)).map(_.id)
        supplier.copy(id = existingId)
      }
      toUpdatesWithCount(extractedWithIds)
    }
  }

  private def toUpdatesWithCount(suppliers: Seq[SupplierUpdate]): UpdatesWithCount[SupplierUpdate] =
    UpdatesWithCount(
      updates = suppliers.map(c => c.copy(id = c.id.orElse(Some(UUID.randomUUID)))),
      toAdd = suppliers.count(_.id.isEmpty),
      toUpdate = suppliers.count(_.id.isDefined),
    )

  protected def toSupplierUpdate(name: String)(implicit importRecord: ImportRecord) =
    SupplierUpdate(
      id = None,
      merchantId = Some(importRecord.merchantId),
      name = Some(name),
      contact = None,
      address = None,
      secondaryAddress = None,
      email = None,
      phoneNumber = None,
      secondaryPhoneNumber = None,
      accountNumber = None,
      notes = None,
      deletedAt = None,
    )

}
