package io.paytouch.core.async.importers.extractors

import java.util.UUID
import cats.implicits._

import io.paytouch.core.async.importers.parsers.{ EnrichedDataRow, ValidationError }
import io.paytouch.core.async.importers.{ Keys, UpdatesWithCount }
import io.paytouch.core.data.model.{ CatalogRecord, CategoryUpdate, ImportRecord }
import io.paytouch.core.entities.enums.CatalogType
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait CategoryExtractor extends Extractor {

  val catalogDao = daos.catalogDao
  val systemCategoryDao = daos.systemCategoryDao

  def extractCategories(
      data: Seq[EnrichedDataRow],
    )(implicit
      importRecord: ImportRecord,
    ): Future[Extraction[CategoryUpdate]] = {
    logExtraction("categories")
    catalogDao.findByMerchantIdAndType(importRecord.merchantId, CatalogType.DefaultMenu).flatMap {
      case Some(defaultMenu) =>
        val extractions = data.map(extractCategoriesPerRow(_, defaultMenu))
        enrichCategories(MultipleExtraction.sequence(extractions))
      case _ =>
        val message = s"Missing default menu for merchant ${importRecord.merchantId}"
        logger.error(message)
        Future.successful(MultipleExtraction.failure(ValidationError(None, message)))
    }
  }

  private def extractCategoriesPerRow(
      row: EnrichedDataRow,
      defaultMenu: CatalogRecord,
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[Seq[CategoryUpdate]] = {
    val names = row.getOrElse(Keys.Category, Seq.empty).filter(_.trim.nonEmpty)
    val categories = names.map(n => toCategoryUpdate(name = n, catalogId = defaultMenu.id.some))
    MultipleExtraction.success(categories)
  }

  protected def enrichCategories(
      extractedCats: ErrorsOr[Seq[CategoryUpdate]],
    )(implicit
      importRecord: ImportRecord,
    ): Future[Extraction[CategoryUpdate]] = {
    val names = extractedCats.getOrElse(Seq.empty).flatMap(_.name)
    for {
      existingCats <- systemCategoryDao.findByNamesAndMerchantId(names, importRecord.merchantId)
    } yield extractedCats.map { extracted =>
      val extractedWithIds = extracted.filterNot(_.name.contains("")).distinctBy(_.name).map { category =>
        val existingId = existingCats.find(c => category.name.contains(c.name)).map(_.id)
        category.copy(id = existingId)
      }
      toUpdatesWithCount(extractedWithIds)
    }
  }

  private def toUpdatesWithCount(categories: Seq[CategoryUpdate]): UpdatesWithCount[CategoryUpdate] =
    UpdatesWithCount(
      updates = categories.map(c => c.copy(id = c.id.orElse(Some(UUID.randomUUID)))),
      toAdd = categories.count(_.id.isEmpty),
      toUpdate = categories.count(_.id.isDefined),
    )

  protected def toCategoryUpdate(
      name: String,
      catalogId: Option[UUID],
      parentId: Option[UUID] = None,
    )(implicit
      importRecord: ImportRecord,
    ) =
    CategoryUpdate(
      id = None,
      name = name.some,
      merchantId = importRecord.merchantId.some,
      catalogId = catalogId,
      parentCategoryId = parentId,
    )
}
