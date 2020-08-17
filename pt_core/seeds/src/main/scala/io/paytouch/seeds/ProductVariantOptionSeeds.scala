package io.paytouch.seeds

import io.paytouch.core.SequenceOfOptionIds
import io.paytouch.core.data.model._

import scala.concurrent._

object ProductVariantOptionSeeds extends Seeds {

  lazy val productVariantOptionDao = daos.productVariantOptionDao

  def load(
      variantOptions: Seq[VariantOptionRecord],
      variantProducts: Seq[ArticleRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[ProductVariantOptionRecord]] = {

    val variantProductsPerTemplate =
      variantProducts.filter(_.isVariantOfProductId.isDefined).groupBy(_.isVariantOfProductId.get).toSeq

    val productVariantOptions = variantProductsPerTemplate.flatMap {
      case (templateId, variants) =>
        val optionsPerTemplate = variantOptions.filter(_.productId == templateId)
        generateProductVariantOptionsPerTemplate(optionsPerTemplate, variants)
    }

    productVariantOptionDao.bulkUpsertByRelIds(productVariantOptions).extractRecords
  }

  private def generateProductVariantOptionsPerTemplate(
      options: Seq[VariantOptionRecord],
      variantProducts: Seq[ArticleRecord],
    )(implicit
      user: UserRecord,
    ): Seq[ProductVariantOptionUpdate] = {
    val optionsPerType = options.groupBy(_.variantOptionTypeId).values
    val allCombinations = optionsPerType.combine

    allCombinations.zip(variantProducts).flatMap {
      case (optionsPerProduct, product) =>
        optionsPerProduct.map { option =>
          ProductVariantOptionUpdate(
            id = None,
            merchantId = Some(user.merchantId),
            variantOptionId = Some(option.id),
            productId = Some(product.id),
          )
        }
    }
  }
}
