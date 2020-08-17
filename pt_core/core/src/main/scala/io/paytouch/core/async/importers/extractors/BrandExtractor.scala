package io.paytouch.core.async.importers.extractors

import java.util.UUID

import io.paytouch.core.async.importers.parsers.EnrichedDataRow
import io.paytouch.core.async.importers.{ Keys, UpdatesWithCount }
import io.paytouch.core.data.model._
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait BrandExtractor extends Extractor {

  val brandDao = daos.brandDao

  def extractBrands(
      data: Seq[EnrichedDataRow],
    )(implicit
      importRecord: ImportRecord,
    ): Future[Extraction[BrandUpdate]] = {
    logExtraction("brands")
    val extractions = data.map(extractBrandsPerRow)
    enrichBrands(MultipleExtraction.sequence(extractions))
  }

  private def extractBrandsPerRow(
      row: EnrichedDataRow,
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[Seq[BrandUpdate]] = {
    val names = row.getOrElse(Keys.Brand, Seq.empty).filter(_.trim.nonEmpty)
    val brands = names.map(n => toBrandUpdate(name = n))
    MultipleExtraction.success(brands)
  }

  protected def enrichBrands(
      extractedBrands: ErrorsOr[Seq[BrandUpdate]],
    )(implicit
      importRecord: ImportRecord,
    ): Future[Extraction[BrandUpdate]] = {
    val names = extractedBrands.getOrElse(Seq.empty).flatMap(_.name)
    for {
      existingBrands <- brandDao.findByNamesAndMerchantId(names, importRecord.merchantId)
    } yield extractedBrands.map { extracted =>
      val extractedWithIds = extracted.filterNot(_.name.contains("")).distinctBy(_.name).map { brand =>
        val existingId = existingBrands.find(c => brand.name.contains(c.name)).map(_.id)
        brand.copy(id = existingId)
      }
      toUpdatesWithCount(extractedWithIds)
    }
  }

  private def toUpdatesWithCount(brands: Seq[BrandUpdate]): UpdatesWithCount[BrandUpdate] =
    UpdatesWithCount(
      updates = brands.map(c => c.copy(id = c.id.orElse(Some(UUID.randomUUID)))),
      toAdd = brands.count(_.id.isEmpty),
      toUpdate = brands.count(_.id.isDefined),
    )

  protected def toBrandUpdate(name: String)(implicit importRecord: ImportRecord) =
    BrandUpdate(id = None, name = Some(name), merchantId = Some(importRecord.merchantId))

}
