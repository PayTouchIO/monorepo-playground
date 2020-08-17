package io.paytouch.seeds

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType }
import io.paytouch.core.entities.{ ResettableBigDecimal, ResettableString }

import scala.concurrent._
import scala.math._

object VariantProductSeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val productDao = daos.productDao

  def load(templateProducts: Seq[ArticleRecord])(implicit user: UserRecord): Future[Seq[ArticleRecord]] = {

    val VariantsPerTemplate = pow(VariantOptionsPerVariantType, VariantOptionTypePerTemplate).toInt

    val variantProducts = templateProducts.flatMap { templateProduct =>
      val variantProductIds = (1 to VariantsPerTemplate).map { idx =>
        s"Variant Product ${templateProduct.id} $idx".toUUID
      }

      variantProductIds.zipWithIndex.map {
        case (variantProductId, idx) =>
          ArticleUpdate(
            id = Some(variantProductId),
            `type` = Some(ArticleType.Variant),
            isCombo = Some(false),
            merchantId = Some(user.merchantId),
            name = Some(s"${templateProduct.name} $idx"),
            description = templateProduct.description,
            brandId = templateProduct.brandId,
            priceAmount = Some(templateProduct.priceAmount),
            costAmount = templateProduct.costAmount,
            averageCostAmount = None,
            unit = Some(templateProduct.unit),
            upc = randomUpc,
            sku = s"$randomWord-$variantProductId",
            isVariantOfProductId = Some(templateProduct.id),
            hasVariant = Some(false),
            trackInventory = Some(templateProduct.trackInventory),
            applyPricingToAllLocations = Some(templateProduct.applyPricingToAllLocations),
            orderRoutingBar = Some(templateProduct.orderRoutingBar),
            orderRoutingKitchen = Some(templateProduct.orderRoutingKitchen),
            orderRoutingEnabled = Some(templateProduct.orderRoutingEnabled),
            discountable = Some(templateProduct.discountable),
            active = Some(templateProduct.active),
            avatarBgColor = templateProduct.avatarBgColor,
            margin = templateProduct.margin,
            isService = Some(templateProduct.isService),
            trackInventoryParts = None,
            hasParts = None,
            scope = Some(ArticleScope.Product),
            deletedAt = templateProduct.deletedAt,
          )
      }
    }

    productDao.bulkUpsert(variantProducts).extractRecords
  }
}
