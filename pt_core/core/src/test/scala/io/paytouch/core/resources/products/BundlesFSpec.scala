package io.paytouch.core.resources.products

import java.util.UUID

import io.paytouch.core.data.model.enums.ArticleScope
import io.paytouch.core.entities.{ BundleCreation, BundleUpdate }

abstract class BundlesFSpec extends ArticlesFSpec {

  abstract class BundleResourceFSpecContext extends ArticleResourceFSpecContext {
    val productDao = daos.productDao

    def assertCreation(
        creation: BundleCreation,
        bundleId: UUID,
        categories: Option[Seq[UUID]] = None,
        suppliers: Option[Seq[UUID]] = None,
      ) = {
      val articleCreation = BundleCreation.convert(creation)(userContext)
      assertArticleCreation(
        articleCreation,
        ArticleScope.Product,
        bundleId,
        systemCategories = categories,
        suppliers = suppliers,
      )
    }

    def assertUpdate(
        update: BundleUpdate,
        bundleId: UUID,
        categories: Option[Seq[UUID]] = None,
        suppliers: Option[Seq[UUID]] = None,
      ): Unit = {
      val articleUpdate = BundleUpdate.convert(update)(userContext)
      assertArticleUpdate(
        articleUpdate,
        ArticleScope.Product,
        bundleId,
        systemCategories = categories,
        suppliers = suppliers,
      )
    }
  }
}
