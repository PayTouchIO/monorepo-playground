package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.{ Keys, Rows, UpdatesWithCount }
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.data.model.{ ArticleUpdate, BrandUpdate, ImportRecord, LocationRecord }
import io.paytouch.core.utils.MultipleExtraction

import scala.concurrent._

trait VariantProductExtractor extends ProductExtractor {

  def extractVariantProducts(
      data: Rows,
      templates: Seq[ArticleUpdate],
      brands: Seq[BrandUpdate],
    )(implicit
      importRecord: ImportRecord,
      locations: Seq[LocationRecord],
    ): Future[ExtractionWithData[ArticleUpdate]] = {
    logExtraction("variant products")
    val variantData = data.filter(row => row.contains(Keys.VariantOptionType) || row.contains(Keys.VariantOption))
    val variantDataByProductName = variantData.groupBy(_.get(Keys.ProductName).flatMap(_.headOption))

    // TODO: this is very inefficient because it handles variants one template at a time
    variantDataByProductName.foldLeft[Future[ExtractionWithData[ArticleUpdate]]](
      Future.successful(
        (MultipleExtraction.success(UpdatesWithCount(updates = Seq.empty, toAdd = 0, toUpdate = 0)), variantData),
      ),
    ) {
      case (result, (productName, variantProductsData)) =>
        result.flatMap {
          case (extraction, updatedData) =>
            val templateId = templates.find(_.name == productName).flatMap(_.id)
            extractProducts(variantProductsData, ArticleType.Variant, templateId, brands).map {
              case (newExtraction, newUpdatedData) =>
                val updatedExtr = MultipleExtraction.combine(extraction, newExtraction)(_ + _)
                val finalData = updatedData ++ newUpdatedData
                (updatedExtr, finalData)
            }
        }
    }
  }
}
