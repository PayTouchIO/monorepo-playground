package io.paytouch.core.async.importers.extractors

import java.util.UUID

import io.paytouch.core.data.daos.ProductDao
import io.paytouch.core.data.model._
import io.paytouch.core.utils.MultipleExtraction
import io.paytouch.core.utils.MultipleExtraction.ErrorsOr

import scala.concurrent._

trait MissingVariantProductExtractor extends ProductVariantOptionExtractor {

  val productDao: ProductDao

  def extractMissingVariantProductsWithOptions(
      variantOptions: Seq[VariantOptionUpdate],
      productVariantOptions: Seq[ProductVariantOptionUpdate],
      variantProducts: Seq[ArticleUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): Future[(ErrorsOr[Seq[ArticleUpdate]], ErrorsOr[Seq[ProductVariantOptionUpdate]])] = {
    logExtraction("missing variant products")
    val extractions = variantOptions.groupBy(_.productId).map {
      case (templateId, variantOptionsPerTemplate) =>
        val variantOptionIdsPerTemplate = variantOptionsPerTemplate.flatMap(_.id)
        val productVariantOptionPerTemplate =
          productVariantOptions.filter(_.variantOptionId.exists(variantOptionIdsPerTemplate.contains))
        extractMissingVariantsPerTemplate(
          templateId,
          variantOptionsPerTemplate,
          productVariantOptionPerTemplate,
          variantProducts,
        )
    }
    val extractedVariantPrds = MultipleExtraction.sequence(extractions.map(_.map { case (vps, _) => vps }))
    val extractedPrdVariantOpts = MultipleExtraction.sequence(extractions.map(_.map { case (_, pvos) => pvos }))

    Future.successful {
      (extractedVariantPrds, extractedPrdVariantOpts)
    }
  }

  private def extractMissingVariantsPerTemplate(
      mainProductId: Option[UUID],
      variantOptions: Seq[VariantOptionUpdate],
      productVariantOptions: Seq[ProductVariantOptionUpdate],
      variantProducts: Seq[ArticleUpdate],
    )(implicit
      importRecord: ImportRecord,
    ): ErrorsOr[(Seq[ArticleUpdate], Seq[ProductVariantOptionUpdate])] = {
    val existingVariantOptionIds =
      productVariantOptions.groupBy(_.productId).values.map(_.flatMap(_.variantOptionId).toSet)
    val variantOptionTypes = variantOptions.flatMap(_.variantOptionTypeId).distinct
    val validCombinationIds = {
      val allCombinations = variantOptions.toSet.subsets(variantOptionTypes.size)
      val uniqueByType =
        allCombinations.map(_.toSeq.distinctBy(_.variantOptionTypeId)).filter(_.size == variantOptionTypes.size).toSeq
      uniqueByType.map(_.flatMap(_.id).toSet)
    }
    val disabledVarProductsWithOptions = validCombinationIds.flatMap { varOptCombination =>
      val alreadyImportedCombination = existingVariantOptionIds.exists(_ == varOptCombination)
      if (!alreadyImportedCombination) {
        val disabledProducts = toDisabledProductUpdates(mainProductId, variantProducts)
        val options = toProductVariantOptionUpdates(disabledProducts, varOptCombination)
        Some(disabledProducts, options)
      }
      else None
    }
    val products = disabledVarProductsWithOptions.flatMap { case (p, _) => p }
    val options = disabledVarProductsWithOptions.flatMap { case (_, pvo) => pvo }
    MultipleExtraction.success((products, options))
  }

  private def toDisabledProductUpdates(
      mainProductId: Option[UUID],
      variantProducts: Seq[ArticleUpdate],
    ): Seq[ArticleUpdate] =
    variantProducts
      .find(_.isVariantOfProductId == mainProductId)
      .map(p => p.copy(id = Some(UUID.randomUUID), active = Some(false), upc = None, sku = None))
      .toSeq

  private def toProductVariantOptionUpdates(
      disableProducts: Seq[ArticleUpdate],
      variantOptionIds: Set[UUID],
    )(implicit
      importRecord: ImportRecord,
    ): Seq[ProductVariantOptionUpdate] =
    disableProducts.flatMap { prd =>
      variantOptionIds.map { varOpId =>
        ProductVariantOptionUpdate(
          id = Some(UUID.randomUUID),
          merchantId = Some(importRecord.merchantId),
          productId = prd.id,
          variantOptionId = Some(varOpId),
        )
      }
    }
}
