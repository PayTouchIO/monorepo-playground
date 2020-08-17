package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object ProductLocationSeeds extends Seeds {

  lazy val productLocationDao = daos.productLocationDao

  def load(
      products: Seq[ArticleRecord],
      locations: Seq[LocationRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[ProductLocationRecord]] = {

    val mainProducts = products.filter(_.`type`.isMain)
    val variantProducts = products.filter(_.`type`.isVariant)

    val productLocations = mainProducts.flatMap { mainProduct =>
      val locationsPerMainProduct = locations.randomAtLeast(1)
      val variants = variantProducts.filter(_.isVariantOfProductId.contains(mainProduct.id))

      (mainProduct +: variants).flatMap { product =>
        locationsPerMainProduct.map { location =>
          ProductLocationUpdate(
            id = None,
            merchantId = Some(user.merchantId),
            productId = Some(product.id),
            locationId = Some(location.id),
            priceAmount = Some(genBigDecimal.instance),
            unit = Some(product.unit),
            costAmount = Some(genBigDecimal.instance),
            averageCostAmount = None,
            margin = Some(genBigDecimal.instance),
            active = None,
            routeToKitchenId = None,
          )
        }
      }
    }

    productLocationDao.bulkUpsertByRelIds(productLocations).extractRecords
  }
}
