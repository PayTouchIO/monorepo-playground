package io.paytouch.core.async.importers.extractors

import java.util.UUID

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.EnrichedDataRow
import io.paytouch.core.data.model._
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait SupplierProductExtractor extends Extractor {

  def extractProductSuppliers(
      data: Seq[EnrichedDataRow],
      suppliers: Seq[SupplierUpdate],
      products: Seq[ArticleUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): Future[ErrorsOr[Seq[SupplierProductUpdate]]] = {
    logExtraction("product suppliers")
    val supplierProductUpdates = data
      .filter(_.contains(Keys.Supplier))
      .flatMap { row =>
        for {
          productId <- row.mainArticleIds
          supplierId <- findSupplierPerRow(row, suppliers).flatMap(_.id)
        } yield SupplierProductUpdate(
          id = Some(UUID.randomUUID),
          merchantId = Some(importRecord.merchantId),
          supplierId = Some(supplierId),
          productId = Some(productId),
        )
      }
    Future.successful(MultipleExtraction.success(supplierProductUpdates))
  }

  protected def findSupplierPerRow(row: EnrichedDataRow, suppliers: Seq[SupplierUpdate]): Option[SupplierUpdate] = {
    val names = row.filterKeys(_ == Keys.Supplier).flatMap { case (_, v) => v }.toSeq

    suppliers.find(_.name.matches(names))
  }
}
