package io.paytouch.core.resources.stocks

import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

abstract class StocksFSpec extends FSpec {

  abstract class StockResourceFSpecContext extends FSpecContext with MultipleLocationFixtures {
    val productDao = daos.productDao
    val productLocationDao = productDao.productLocationDao
    val stockDao = daos.stockDao

    val simple = Factory.simpleProduct(merchant).create
    val simpleLondon = Factory.productLocation(simple, london).create

    val template = Factory.templateProduct(merchant).create
    val variant1 = Factory.variantProduct(merchant, template).create
    val variant2 = Factory.variantProduct(merchant, template).create
    val variantDeleted = Factory.variantProduct(merchant, template, deletedAt = Some(UtcTime.now)).create

    val variant1London = Factory.productLocation(variant1, london).create
    val variant2London = Factory.productLocation(variant2, london).create
    val variantDeletedLondon = Factory.productLocation(variantDeleted, london).create

    def assertBulkCreation(creations: Seq[StockCreation], stocks: Seq[Stock]) =
      assertBulkUpdate(creations.map(_.asUpdate))

    def assertBulkUpdate(updates: Seq[StockUpdate]) =
      updates.map(assertUpdate)

    def assertUpdate(update: StockUpdate) = {
      val stock =
        stockDao
          .findByProductIdAndLocationIds(merchant.id, update.productId, Seq(update.locationId))
          .await
          .get

      stock.locationId ==== update.locationId
      stock.productId ==== update.productId

      if (update.quantity.isDefined) update.quantity ==== Some(stock.quantity)
      if (update.minimumOnHand.isDefined) update.minimumOnHand ==== Some(stock.minimumOnHand)
      if (update.reorderAmount.isDefined) update.reorderAmount ==== Some(stock.reorderAmount)
      if (update.sellOutOfStock.isDefined) update.sellOutOfStock ==== Some(stock.sellOutOfStock)
    }

    def assertBulkResponse(stocks: Seq[Stock]) =
      stocks.foreach(assertResponse)

    def assertResponse(stock: Stock) = {
      val dbStock =
        stockDao
          .findByProductIdAndLocationIds(merchant.id, stock.productId, Seq(stock.locationId))
          .await
          .get

      stock.locationId ==== dbStock.locationId
      stock.productId ==== dbStock.productId
      stock.quantity ==== dbStock.quantity
      stock.minimumOnHand ==== dbStock.minimumOnHand
      stock.reorderAmount ==== dbStock.reorderAmount
      stock.sellOutOfStock ==== dbStock.sellOutOfStock
    }
  }
}
