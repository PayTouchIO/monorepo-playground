package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.Keys
import io.paytouch.core.async.importers.parsers.EnrichedDataRow
import io.paytouch.core.conversions.ProductCategoryConversions
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.upsertions.ProductCategoryUpsertion
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait ProductCategoryExtractor extends Extractor with ProductCategoryConversions {

  def extractProductCategories(
      data: Seq[EnrichedDataRow],
      categories: Seq[CategoryUpdate],
      subcategories: Seq[CategoryUpdate],
      products: Seq[ArticleUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): Future[ErrorsOr[Seq[ProductCategoryUpsertion]]] = {
    logExtraction("product categories")
    val productCategories = data
      .filter(r => r.contains(Keys.Category) || r.contains(Keys.Subcategory))
      .flatMap { row =>
        val productIds = row.mainArticleIds
        val categoriesPerRow = findMainCategoriesPerRow(row, categories)
        val subcategoriesPerRow = findSubcategoriesPerRow(row, categoriesPerRow, subcategories)
        val categoriesToMatchIds =
          selectCategoriesToMatchWithProduct(categoriesPerRow, subcategoriesPerRow).flatMap(_.id)
        val merchantId = importRecord.merchantId
        toProductCategoryUpsertions(
          merchantId = merchantId,
          productIds = productIds,
          categoryIds = categoriesToMatchIds,
        )
      }
      .distinctBy(pc => (pc.productCategory.productId, pc.productCategory.categoryId))
    Future.successful(MultipleExtraction.success(productCategories))
  }

  private def selectCategoriesToMatchWithProduct(
      categories: Seq[CategoryUpdate],
      subcategories: Seq[CategoryUpdate],
    ): Seq[CategoryUpdate] = {
    val mainCategoriesWithNoSub = categories.filterNot { cat =>
      subcategories.exists(subcat => subcat.parentCategoryId == cat.id)
    }
    mainCategoriesWithNoSub ++ subcategories
  }
}
