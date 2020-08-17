package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.ArticleScope
import io.paytouch.core.utils.UtcTime

import scala.concurrent.ExecutionContext

class ProductDao(
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

  lazy val scope = Some(ArticleScope.Product)

  def orderItemDao = anonymOrderItemDao
  def stockDao = anonymStockDao

  def updateAverageCosts(productAverageCosts: Map[UUID, BigDecimal]) = {
    val updates = productAverageCosts.map {
      case (productId, averageCost) => queryUpdateAverageCost(productId, averageCost)
    }
    runWithTransaction(asTraversable(updates.toSeq))
  }

  private def queryUpdateAverageCost(productId: UUID, averageCost: BigDecimal) = {
    val field = for { o <- table if o.id === productId } yield (o.averageCostAmount, o.updatedAt)
    field.update(Some(averageCost), UtcTime.now)
  }
}
