package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, UnitType }
import io.paytouch.core.entities.{ ResettableBigDecimal, ResettableString }
import io.paytouch.seeds.IdsProvider._

import scala.concurrent._

object SimpleProductSeeds extends Seeds {

  lazy val productDao = daos.productDao

  def load(brands: Seq[BrandRecord])(implicit user: UserRecord): Future[Seq[ArticleRecord]] = {
    val simpleProductIds = simpleProductIdsPerEmail(user.email)

    val simpleProducts = simpleProductIds.map { simpleProductId =>
      val productName = randomWords
      ArticleUpdate(
        id = Some(simpleProductId),
        `type` = Some(ArticleType.Simple),
        isCombo = Some(false),
        merchantId = Some(user.merchantId),
        name = Some(productName),
        description = Some(randomWords(n = 15, allCapitalized = false)),
        brandId = Some(brands.random.id),
        priceAmount = Some(genBigDecimal.instance),
        costAmount = Some(genBigDecimal.instance),
        averageCostAmount = None,
        unit = Some(UnitType.`Unit`),
        upc = randomUpc,
        sku = s"${productName.replace(" ", "-")}-sku-$simpleProductId",
        isVariantOfProductId = Some(simpleProductId),
        hasVariant = Some(false),
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

    productDao.bulkUpsert(simpleProducts).extractRecords
  }
}
