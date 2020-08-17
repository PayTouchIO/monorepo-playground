package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object ProductCategorySeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val productCategoryDao = daos.productCategoryDao

  def load(
      products: Seq[ArticleRecord],
      categories: Seq[CategoryRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[ProductCategoryRecord]] = {

    val productsWithCategories = products.random(ProductsWithCategory)

    val productCategories = productsWithCategories.flatMap { product =>
      categories.randomSample.map { category =>
        ProductCategoryUpdate(
          id = None,
          merchantId = Some(user.merchantId),
          productId = Some(product.id),
          categoryId = Some(category.id),
          position = None,
        )
      }
    }

    productCategoryDao.bulkUpsertByRelIds(productCategories).extractRecords
  }
}
