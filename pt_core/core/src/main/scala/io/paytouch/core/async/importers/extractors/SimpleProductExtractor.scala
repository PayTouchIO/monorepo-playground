package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.EnrichedDataRow
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.data.model.{ ArticleUpdate, BrandUpdate, ImportRecord, LocationRecord }

import scala.concurrent._

trait SimpleProductExtractor extends ProductExtractor {

  def extractSimpleProducts(
      data: Seq[EnrichedDataRow],
      brands: Seq[BrandUpdate],
    )(implicit
      importRecord: ImportRecord,
      locations: Seq[LocationRecord],
    ): Future[ExtractionWithData[ArticleUpdate]] = {
    logExtraction("simple products")
    val simpleData = data.filterNot(row => row.contains(Keys.VariantOptionType) || row.contains(Keys.VariantOption))
    extractProducts(simpleData, ArticleType.Simple, None, brands)
  }
}
