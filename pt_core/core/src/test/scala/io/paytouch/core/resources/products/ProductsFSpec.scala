package io.paytouch.core.resources.products

import java.util.UUID

import io.paytouch.core.data.model.enums.ArticleScope
import io.paytouch.core.entities.enums.MerchantSetupSteps
import io.paytouch.core.entities.{ ProductCreation, ProductUpdate }
import io.paytouch.core.utils.SetupStepsAssertions

abstract class ProductsFSpec extends ArticlesFSpec {

  abstract class ProductResourceFSpecContext extends ArticleResourceFSpecContext with SetupStepsAssertions {
    val productDao = daos.productDao
    val modifierSetDao = daos.modifierSetDao

    def assertCreation(
        creation: ProductCreation,
        productId: UUID,
        categories: Option[Seq[UUID]] = None,
        suppliers: Option[Seq[UUID]] = None,
      ) = {
      val articleCreation = ProductCreation.convert(creation)(userContext)
      assertArticleCreation(
        articleCreation,
        ArticleScope.Product,
        productId,
        systemCategories = categories,
        suppliers = suppliers,
      )
      assertSetupStepCompleted(merchant, MerchantSetupSteps.ImportProducts)
    }

    def assertUpdate(
        update: ProductUpdate,
        productId: UUID,
        categories: Option[Seq[UUID]] = None,
        suppliers: Option[Seq[UUID]] = None,
      ): Unit = {
      val articleUpdate = ProductUpdate.convert(update)(userContext)
      assertArticleUpdate(
        articleUpdate,
        ArticleScope.Product,
        productId,
        systemCategories = categories,
        suppliers = suppliers,
      )
    }
  }
}
