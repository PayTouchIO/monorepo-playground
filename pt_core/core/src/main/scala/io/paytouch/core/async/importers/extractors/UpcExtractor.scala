package io.paytouch.core.async.importers.extractors

import scala.concurrent._

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers._
import io.paytouch.core.data.daos.ProductDao
import io.paytouch.core.data.model._
import io.paytouch.core.utils._

trait UpcExtractor extends Extractor {
  val productDao: ProductDao

  def extractUpcs(
      data: Seq[EnrichedDataRow],
      products: Seq[ArticleUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): Future[MultipleExtraction.ErrorsOr[Seq[String]]] = {
    logExtraction("upcs")
    val upcsPerLine = data
      .map(row => (row.rowNumber, row.getOrElse(Keys.Upc, Seq.empty)))
      .filter {
        case (_, upcs) => upcs.nonEmpty
      }
      .toMap
    for {
      validatedWithImportedUpcs <- Future.successful(validateWithImportedUpcs(upcsPerLine))
      validatedWithExistingUpcs <- validateWithExistingUpcs(upcsPerLine, products)
    } yield MultipleExtraction.combine(validatedWithImportedUpcs, validatedWithExistingUpcs) {
      case (a, b) => (a ++ b).distinct
    }
  }

  private def validateWithImportedUpcs(upcsPerLine: Map[Int, Seq[String]]): MultipleExtraction.ErrorsOr[Seq[String]] = {
    def validateUpc(
        l: Int,
        upc: String,
        upcsPerLine: Map[Int, Seq[String]],
      ): MultipleExtraction.ErrorsOr[Seq[String]] = {
      val otherUpcsPerLine = upcsPerLine.view.filterKeys(_ != l).toMap
      val linesWithDuplicates = otherUpcsPerLine.flatMap {
        case (line, otherUpcs) =>
          if (otherUpcs.contains(upc)) Some(line)
          else None
      }.toSet
      if (linesWithDuplicates.nonEmpty)
        MultipleExtraction.failure(
          ValidationError(Some(l), s"Duplicated UPC '$upc' (see line ${linesWithDuplicates.mkString(", ")})."),
        )
      else MultipleExtraction.success(Seq(upc))
    }

    val validatedUpcs = upcsPerLine.flatMap {
      case (line, upcs) =>
        upcs.map(validateUpc(line, _, upcsPerLine))
    }
    MultipleExtraction.sequence(validatedUpcs)
  }

  private def validateWithExistingUpcs(
      upcsPerLine: Map[Int, Seq[String]],
      products: Seq[ArticleUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): Future[MultipleExtraction.ErrorsOr[Seq[String]]] = {
    val productsToUpdateIds = products.flatMap(_.id)
    val upcs = upcsPerLine.values.flatten.toSeq
    productDao.findAllByUpcsAndMerchantId(importRecord.merchantId, upcs).map { existingProductsWithUpc =>
      val otherProductsWithUpc = existingProductsWithUpc.filterNot(p => productsToUpdateIds.contains(p.id))
      val otherUpcs = otherProductsWithUpc.flatMap(_.upc)
      MultipleExtraction.sequence {
        upcsPerLine.flatMap {
          case (line, upcs) =>
            upcs.map(validateUpcNotExists(line, _, otherUpcs))
        }
      }
    }
  }

  private def validateUpcNotExists(
      line: Int,
      upc: String,
      otherExistingUpcs: Seq[String],
    )(implicit
      importRecord: ImportRecord,
    ): MultipleExtraction.ErrorsOr[Seq[String]] =
    if (!importRecord.deleteExisting && otherExistingUpcs.contains(upc))
      MultipleExtraction.failure(
        ValidationError(
          Some(line),
          s"Another product with UPC '$upc' already exists. Please, provide a different UPC.",
        ),
      )
    else
      MultipleExtraction.success(Seq(upc))
}
