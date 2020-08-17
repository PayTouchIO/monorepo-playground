package io.paytouch.core.async.importers.extractors

import cats.data.ValidatedNel
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.async.importers.parsers.{ EnrichedDataRow, ValidationError }
import io.paytouch.core.async.importers.{ Keys, Rows, UpdatesWithCount }
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr
import io.paytouch.core.utils.{ Implicits, MultipleExtraction }

import scala.util.Try

trait Extractor extends LazyLogging with Implicits {

  type Extraction[T] = ValidatedNel[ValidationError, UpdatesWithCount[T]]
  type ExtractionWithData[T] = (Extraction[T], Rows)

  def logExtraction(context: String)(implicit importRecord: ImportRecord) =
    logger.info(s"[Import ${importRecord.id}] Starting parsing for $context")

  protected def findMainCategoriesPerRow(row: EnrichedDataRow, categories: Seq[CategoryUpdate]): Seq[CategoryUpdate] = {
    val names = row.filterKeys(k => k == Keys.Category).flatMap { case (_, v) => v }.toSeq

    categories.filter(_.name.matches(names))
  }

  protected def findSubcategoriesPerRow(
      row: EnrichedDataRow,
      mainCategoriesInRow: Seq[CategoryUpdate],
      subcategories: Seq[CategoryUpdate],
    ): Seq[CategoryUpdate] = {
    val names = row.filterKeys(k => k == Keys.Subcategory).flatMap { case (_, v) => v }.toSeq

    val validSubcategories = subcategories.filter { subcat =>
      mainCategoriesInRow.exists(cat => subcat.parentCategoryId == cat.id)
    }

    validSubcategories.filter(_.name.matches(names))
  }

  protected def findVariantOptionTypesPerRow(
      row: EnrichedDataRow,
      variantOptionTypes: Seq[VariantOptionTypeUpdate],
    ): Seq[VariantOptionTypeUpdate] = {
    val names = row.filterKeys(k => k == Keys.VariantOptionType).flatMap { case (_, v) => v }.toSeq

    for {
      variantOptionType <- variantOptionTypes
      if variantOptionType.productId == row.templateArticleId
      if variantOptionType.name.matches(names)
    } yield variantOptionType
  }

  protected def findProductLocationsPerRow(
      row: EnrichedDataRow,
      productLocations: Seq[ProductLocationUpdate],
      products: Seq[ArticleUpdate],
    ): Seq[ProductLocationUpdate] =
    productLocations.filter(_.productId.matches(row.mainArticleIds))

  protected def containsStockData(row: EnrichedDataRow): Boolean =
    row.keys.exists(k => k == Keys.StockQuantity || k == Keys.StockMinimumOnHand)

  protected def extractInt(row: EnrichedDataRow, key: String): ErrorsOr[Int] =
    extractInts(row, key).map(_.head)

  protected def extractInts(row: EnrichedDataRow, key: String): ErrorsOr[List[Int]] =
    extractNumbers(row, key)(_.toInt)

  protected def extractBigDecimalWithDefault(
      row: EnrichedDataRow,
      key: String,
    )(
      default: BigDecimal,
    ): ErrorsOr[BigDecimal] =
    if (row.get(key).isDefined) extractBigDecimal(row, key)
    else MultipleExtraction.success(default)

  protected def extractBigDecimal(row: EnrichedDataRow, key: String): ErrorsOr[BigDecimal] =
    extractBigDecimals(row, key).map(_.head)

  protected def extractBigDecimals(row: EnrichedDataRow, key: String): ErrorsOr[List[BigDecimal]] =
    extractNumbers(row, key)(BigDecimal(_))

  private def extractNumbers[N](row: EnrichedDataRow, key: String)(f: String => N): ErrorsOr[List[N]] =
    row.get(key) match {
      case Some(values) if values.nonEmpty =>
        Try(MultipleExtraction.success(values.map(f))) getOrElse {
          MultipleExtraction.failure(
            ValidationError(
              Some(row.rowNumber),
              s"${key.capitalize} can't be alphanumeric (was ${values.mkString(", ")})",
            ),
          )
        }
      case _ => MultipleExtraction.failure(ValidationError(Some(row.rowNumber), s"${key.capitalize} is missing"))
    }

  protected def extractString(row: EnrichedDataRow, key: String): ErrorsOr[String] =
    extractStrings(row, key).map(_.head)

  protected def extractStrings(row: EnrichedDataRow, key: String): ErrorsOr[List[String]] =
    row.get(key) match {
      case Some(values) if values.nonEmpty => MultipleExtraction.success(values)
      case _ =>
        MultipleExtraction.failure(ValidationError(line = Some(row.rowNumber), s"${key.capitalize} is missing"))
    }
}
