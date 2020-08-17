package io.paytouch.core.resources.products

import java.util.UUID

import io.paytouch.core.data.model.enums.ArticleScope
import io.paytouch.core.entities.{ PartCreation, PartUpdate }

abstract class PartsFSpec extends ArticlesFSpec {

  abstract class PartResourceFSpecContext extends ArticleResourceFSpecContext {
    val partDao = daos.partDao

    def assertCreation(
        creation: PartCreation,
        productId: UUID,
        categories: Option[Seq[UUID]] = None,
        suppliers: Option[Seq[UUID]] = None,
      ) = {
      val articleCreation = PartCreation.convert(creation)(userContext)
      assertArticleCreation(
        articleCreation,
        ArticleScope.Part,
        productId,
        systemCategories = categories,
        suppliers = suppliers,
      )
    }

    def assertUpdate(
        update: PartUpdate,
        productId: UUID,
        categories: Option[Seq[UUID]] = None,
        suppliers: Option[Seq[UUID]] = None,
      ): Unit = {
      val articleUpdate = PartUpdate.convert(update)(userContext)
      assertArticleUpdate(
        articleUpdate,
        ArticleScope.Part,
        productId,
        systemCategories = categories,
        suppliers = suppliers,
      )
    }
  }
}
