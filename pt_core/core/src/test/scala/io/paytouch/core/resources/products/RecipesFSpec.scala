package io.paytouch.core.resources.products

import java.util.UUID

import io.paytouch.core.data.model.enums.ArticleScope
import io.paytouch.core.entities.{ RecipeCreation, RecipeUpdate }
abstract class RecipesFSpec extends ArticlesFSpec {

  abstract class RecipeResourceFSpecContext extends ArticleResourceFSpecContext {
    val partDao = daos.partDao

    def assertCreation(
        creation: RecipeCreation,
        recipeId: UUID,
        categories: Option[Seq[UUID]] = None,
        suppliers: Option[Seq[UUID]] = None,
      ) = {
      val articleCreation = RecipeCreation.convert(creation)(userContext)
      assertArticleCreation(
        articleCreation,
        ArticleScope.Part,
        recipeId,
        systemCategories = categories,
        suppliers = suppliers,
      )
    }

    def assertUpdate(
        update: RecipeUpdate,
        recipeId: UUID,
        categories: Option[Seq[UUID]] = None,
        suppliers: Option[Seq[UUID]] = None,
      ): Unit = {
      val articleUpdate = RecipeUpdate.convert(update)(userContext)
      assertArticleUpdate(
        articleUpdate,
        ArticleScope.Part,
        recipeId,
        systemCategories = categories,
        suppliers = suppliers,
      )
    }
  }
}
