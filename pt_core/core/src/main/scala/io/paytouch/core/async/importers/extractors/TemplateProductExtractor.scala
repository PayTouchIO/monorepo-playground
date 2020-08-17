package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.{ Keys, Rows }
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.data.model.{ ArticleUpdate, BrandUpdate, ImportRecord, LocationRecord }

import scala.concurrent._

trait TemplateProductExtractor extends ProductExtractor {

  def extractTemplateProducts(
      data: Rows,
      brands: Seq[BrandUpdate],
    )(implicit
      importRecord: ImportRecord,
      locations: Seq[LocationRecord],
    ): Future[ExtractionWithData[ArticleUpdate]] = {
    logExtraction("template products")
    val dataWithVariants = data.filter(row => row.contains(Keys.VariantOptionType) || row.contains(Keys.VariantOption))
    val templateData = dataWithVariants
      .groupBy(_.get(Keys.ProductName))
      .view
      .filterKeys(_.isDefined)
      .toMap
      .map {
        case (_, v) => v.find(containsStockData).getOrElse(v.head)
      }
      .toList
    extractProducts(templateData, ArticleType.Template, None, brands)
  }
}
