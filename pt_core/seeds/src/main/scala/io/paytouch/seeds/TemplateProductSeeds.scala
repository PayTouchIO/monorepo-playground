package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, UnitType }
import io.paytouch.seeds.IdsProvider._

import scala.concurrent._

object TemplateProductSeeds extends Seeds {

  lazy val productDao = daos.productDao

  def load(brands: Seq[BrandRecord])(implicit user: UserRecord): Future[Seq[ArticleRecord]] = {
    val templateProductIds = templateProductIdsPerEmail(user.email)

    val products = templateProductIds.map { templateProductId =>
      val productName = randomWords
      ArticleUpdate(
        id = Some(templateProductId),
        `type` = Some(ArticleType.Template),
        isCombo = Some(false),
        merchantId = Some(user.merchantId),
        name = Some(productName),
        description = Some(randomWords(n = 15, allCapitalized = false)),
        brandId = Some(brands.random.id),
        priceAmount = Some(genBigDecimal.instance),
        costAmount = Some(genBigDecimal.instance),
        averageCostAmount = None,
        unit = Some(UnitType.`Unit`),
        upc = None,
        sku = None,
        isVariantOfProductId = None,
        hasVariant = Some(true),
        trackInventory = Some(genBoolean.instance),
        applyPricingToAllLocations = None,
        orderRoutingBar = None,
        orderRoutingKitchen = None,
        orderRoutingEnabled = None,
        discountable = None,
        active = None,
        avatarBgColor = Some(genColor.instance),
        margin = Some(genBigDecimal.instance),
        isService = None,
        trackInventoryParts = None,
        hasParts = None,
        scope = Some(ArticleScope.Product),
        deletedAt = None,
      )
    }

    productDao.bulkUpsert(products).extractRecords
  }
}
