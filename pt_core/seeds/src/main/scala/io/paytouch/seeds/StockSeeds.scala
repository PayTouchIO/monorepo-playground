package io.paytouch.seeds

import io.paytouch.core.data.model._

import scala.concurrent._

object StockSeeds extends Seeds {

  lazy val stockDao = daos.stockDao

  def load(productLocations: Seq[ProductLocationRecord])(implicit user: UserRecord): Future[Seq[StockRecord]] = {

    val stocks = productLocations.map { productLocation =>
      StockUpdate(
        id = None,
        merchantId = Some(user.merchantId),
        productId = Some(productLocation.productId),
        locationId = Some(productLocation.locationId),
        quantity = Some(genBigDecimal.instance),
        minimumOnHand = Some(genBigDecimal.instance),
        reorderAmount = Some(genBigDecimal.instance),
        sellOutOfStock = None,
      )
    }

    stockDao.bulkUpsert(stocks).extractRecords
  }
}
