package io.paytouch.core.data.daos

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.ArticleScope

import scala.concurrent.ExecutionContext

class PartDao(
    val bundleSetDao: BundleSetDao,
    val imageUploadDao: ImageUploadDao,
    val loyaltyRewardProductDao: LoyaltyRewardProductDao,
    val modifierSetProductDao: ModifierSetProductDao,
    anonymOrderItemDao: => OrderItemDao,
    val productCategoryDao: ProductCategoryDao,
    val productLocationDao: ProductLocationDao,
    val productLocationTaxRateDao: ProductLocationTaxRateDao,
    val recipeDetailDao: RecipeDetailDao,
    val supplierProductDao: SupplierProductDao,
    anonymStockDao: => StockDao,
    val variantOptionTypeDao: VariantOptionTypeDao,
    val articleIdentifierDao: ArticleIdentifierDao,
    val variantOptionDao: VariantOptionDao,
    val variantProductDao: VariantProductDao,
  )(implicit
    override val ec: ExecutionContext,
    override val db: Database,
  ) extends GenericArticleDao {

  lazy val scope = Some(ArticleScope.Part)

  def orderItemDao = anonymOrderItemDao
  def stockDao = anonymStockDao

}
