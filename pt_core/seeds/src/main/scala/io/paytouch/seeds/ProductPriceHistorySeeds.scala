package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object ProductPriceHistorySeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val productPriceHistoryDao = daos.productPriceHistoryDao

  def load(
      products: Seq[ArticleRecord],
      productLocations: Seq[ProductLocationRecord],
      users: Seq[UserRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[ProductPriceHistoryRecord]] = {

    val priceChanges = products.random(ProductsWithHistory).flatMap { product =>
      productLocations.filter(_.productId == product.id).flatMap { productLocation =>
        (1 to ChangesPerProduct).map { _ =>
          ProductPriceHistoryUpdate(
            id = None,
            merchantId = Some(user.merchantId),
            productId = Some(product.id),
            locationId = Some(productLocation.locationId),
            userId = Some(users.random.id),
            date = Some(genZonedDateTimeInThePast.instance),
            prevPriceAmount = Some(genBigDecimal.instance),
            newPriceAmount = Some(genBigDecimal.instance),
            reason = Some(genChangeReason.instance),
            notes = Some(randomWords),
          )
        }
      }
    }

    productPriceHistoryDao.bulkUpsert(priceChanges).extractRecords
  }
}
