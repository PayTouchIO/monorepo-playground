package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object ProductCostHistorySeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val productCostHistoryDao = daos.productCostHistoryDao

  def load(
      products: Seq[ArticleRecord],
      productLocations: Seq[ProductLocationRecord],
      users: Seq[UserRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[ProductCostHistoryRecord]] = {

    val costChanges = products.random(ProductsWithHistory).flatMap { product =>
      productLocations.filter(_.productId == product.id).flatMap { productLocation =>
        (1 to ChangesPerProduct).map { _ =>
          ProductCostHistoryUpdate(
            id = None,
            merchantId = Some(user.merchantId),
            productId = Some(product.id),
            locationId = Some(productLocation.locationId),
            userId = Some(users.random.id),
            date = Some(genZonedDateTimeInThePast.instance),
            prevCostAmount = Some(genBigDecimal.instance),
            newCostAmount = Some(genBigDecimal.instance),
            reason = Some(genChangeReason.instance),
            notes = Some(randomWords),
          )
        }
      }
    }

    productCostHistoryDao.bulkUpsert(costChanges).extractRecords
  }
}
