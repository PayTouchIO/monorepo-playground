package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.{ EnrichedDataRow, ValidationError }
import io.paytouch.core.data.model.{ CategoryUpdate, ImportRecord }
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait SubcategoryExtractor extends Extractor { self: CategoryExtractor =>

  def extractSubcategories(
      data: Seq[EnrichedDataRow],
      categories: Seq[CategoryUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): Future[Extraction[CategoryUpdate]] = {
    logExtraction("subcategories")
    val extractions = data.map(extractSubcategoriesPerRow(_, categories))
    enrichCategories(MultipleExtraction.sequence(extractions))
  }

  private def extractSubcategoriesPerRow(
      row: EnrichedDataRow,
      categories: Seq[CategoryUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[Seq[CategoryUpdate]] = {
    val subcatNames = row.getOrElse(Keys.Subcategory, Seq.empty).filter(_.trim.nonEmpty)
    val catNames = row.getOrElse(Keys.Category, Seq.empty).filter(_.trim.nonEmpty)
    val names = catNames.zipAll(subcatNames, catNames.lastOption.getOrElse(""), subcatNames.lastOption.getOrElse(""))

    val extractions = names.map {
      case (catName, subcatName) =>
        val maybeExistingCategory = categories.find(_.name.contains(catName))
        maybeExistingCategory match {
          case Some(existingCategory) =>
            val catUpdate =
              toCategoryUpdate(subcatName, catalogId = existingCategory.catalogId, parentId = existingCategory.id)
            MultipleExtraction.success(Seq(catUpdate))
          case None =>
            MultipleExtraction.failure(
              ValidationError(Some(row.rowNumber), s"Category not found for sub category $subcatName"),
            )
        }
    }
    MultipleExtraction.sequence(extractions)
  }
}
