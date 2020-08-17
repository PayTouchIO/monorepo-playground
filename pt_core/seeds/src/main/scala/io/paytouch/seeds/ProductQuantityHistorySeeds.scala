package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object ProductQuantityHistorySeeds extends Seeds {
  import SeedsQuantityProvider._

  lazy val productQuantityHistoryDao = daos.productQuantityHistoryDao

  def load(
      products: Seq[ArticleRecord],
      productLocations: Seq[ProductLocationRecord],
      users: Seq[UserRecord],
      orders: Seq[OrderRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[ProductQuantityHistoryRecord]] = {

    val quantityChanges = products.random(ProductsWithHistory).flatMap { product =>
      productLocations.filter(_.productId == product.id).flatMap { productLocation =>
        (1 to ChangesPerProduct).map { _ =>
          ProductQuantityHistoryUpdate(
            id = None,
            merchantId = Some(user.merchantId),
            productId = Some(product.id),
            locationId = Some(productLocation.locationId),
            userId = Some(users.random.id),
            date = Some(genZonedDateTimeInThePast.instance),
            orderId = if (genBoolean.instance) Some(orders.random.id) else None,
            prevQuantityAmount = Some(genBigDecimal.instance),
            newQuantityAmount = Some(genBigDecimal.instance),
            reason = Some(genQuantityChangeReason.instance),
            newStockValueAmount = Some(genBigDecimal.instance),
            notes = Some(randomWords),
          )
        }
      }
    }

    productQuantityHistoryDao.bulkUpsert(quantityChanges).extractRecords
  }
}
