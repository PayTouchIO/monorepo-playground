package io.paytouch.core.async.importers.extractors

import io.paytouch.core.async.importers.{ Keys, Rows }
import io.paytouch.core.async.importers.parsers.EnrichedDataRow
import io.paytouch.core.conversions.ProductVariantOptionConversions
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.ArticleType
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait ProductVariantOptionExtractor extends Extractor with ProductVariantOptionConversions {

  def extractProductVariantOptions(
      data: Rows,
      variantOptions: Seq[VariantOptionUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): Future[ErrorsOr[Seq[ProductVariantOptionUpdate]]] = {
    logExtraction("product variant options")
    val extractions = MultipleExtraction.sequence {
      data
        .filter(_.contains(Keys.VariantOption))
        .map(extractProductVariantOptionsPerRow(_, variantOptions))
    }
    Future.successful(extractions)
  }

  def extractProductVariantOptionsPerRow(
      row: EnrichedDataRow,
      variantOptions: Seq[VariantOptionUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[Seq[ProductVariantOptionUpdate]] = {
    val variantProduct = row.articleUpdateByType(ArticleType.Variant).map(_.update)
    val optionNames = row.getOrElse(Keys.VariantOption, List.empty)
    val templateId = variantProduct.flatMap(_.isVariantOfProductId)
    val variantOptionIds = {
      val options = for {
        variantOption <- variantOptions
        if variantOption.productId == templateId
        if variantOption.name.exists(optionNames.contains)
      } yield variantOption
      options.flatMap(_.id)
    }
    val variantId = variantProduct.flatMap(_.id)
    val extractions = variantOptionIds.flatMap { variantOptionId =>
      variantId.map { varId =>
        ProductVariantOptionUpdate(
          id = None,
          merchantId = Some(importRecord.merchantId),
          productId = Some(varId),
          variantOptionId = Some(variantOptionId),
        )
      }
    }
    MultipleExtraction.success(extractions)
  }
}
