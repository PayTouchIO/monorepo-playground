package io.paytouch.core.data.daos

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.ArticleScope

import scala.concurrent.ExecutionContext

class ArticleDao(
    val bundleSetDao: BundleSetDao,
    val imageUploadDao: ImageUploadDao,
    val loyaltyRewardProductDao: LoyaltyRewardProductDao,
    val modifierSetProductDao: ModifierSetProductDao,
    anonymOrderItemDao: => OrderItemDao,
    val productCategoryDao: ProductCategoryDao,
    anonymProductLocationDao: => ProductLocationDao,
    val productLocationTaxRateDao: ProductLocationTaxRateDao,
    val recipeDetailDao: RecipeDetailDao,
    val supplierProductDao: SupplierProductDao,
    anonymStockDao: => StockDao,
    val variantOptionTypeDao: VariantOptionTypeDao,
    val articleIdentifierDao: ArticleIdentifierDao,
    val variantOptionDao: VariantOptionDao,
    val variantProductDao: VariantProductDao,
  )(implicit
    val ec: ExecutionContext,
    val db: Database,
  ) extends GenericArticleDao {

  lazy val scope: Option[ArticleScope] = None

  def orderItemDao = anonymOrderItemDao
  def productLocationDao = anonymProductLocationDao
  def stockDao = anonymStockDao

}
