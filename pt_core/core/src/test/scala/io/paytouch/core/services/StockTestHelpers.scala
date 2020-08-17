package io.paytouch.core.services

import java.util.UUID

import akka.actor.Props
import com.softwaremill.macwire.wire
import io.paytouch.core.async.sqs.SQSMessageSender
import io.paytouch.core.async.monitors.{ ProductQuantityHistoryMonitor, StockModifierMonitor }
import io.paytouch.core.data.model.enums.QuantityChangeReason
import io.paytouch.core.data.model.{ ProductLocationRecord, StockRecord }
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

trait StockTestHelpers { self: ServiceDaoSpec =>

  trait StockTestHelperContext extends ProductStockFixtures { self: ServiceDaoSpecContext =>
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])
    val productQuantityHistoryMonitor =
      actorSystem.actorOf(Props(wire[ProductQuantityHistoryMonitor])).taggedWith[ProductQuantityHistoryMonitor]
    val stockModifierMonitor = actorSystem.actorOf(Props(wire[StockModifierMonitor])).taggedWith[StockModifierMonitor]

    val stockDao = daos.stockDao
    val productQuantityHistoryDao = daos.productQuantityHistoryDao

    def assertProductHistoryRecordFromOrder(
        orderId: UUID,
        productLocation: ProductLocationRecord,
        oldStock: StockRecord,
        orderedQuantity: BigDecimal,
        reason: QuantityChangeReason = QuantityChangeReason.Sale,
      ) =
      assertProductHistoryRecord(
        productLocation,
        oldStock,
        -orderedQuantity,
        reason,
        notes = None,
        orderId = Some(orderId),
      )

    def assertProductHistoryRecord(
        productLocation: ProductLocationRecord,
        oldStock: StockRecord,
        increasedQuantity: BigDecimal,
        reason: QuantityChangeReason,
        notes: Option[String],
        orderId: Option[UUID] = None,
      ) = {
      val productId = productLocation.productId
      val locationId = productLocation.locationId
      val records = productQuantityHistoryDao.findAllByProductIdsAndLocationId(Seq(productId), locationId).await
      records.size ==== 1
      val record = records.head
      records.head.merchantId ==== merchant.id
      record.productId ==== productId
      record.locationId ==== locationId
      record.userId ==== Some(user.id)
      record.orderId ==== orderId
      record.prevQuantityAmount ==== oldStock.quantity
      record.newQuantityAmount ==== oldStock.quantity + increasedQuantity
      record.newStockValueAmount ==== (oldStock.quantity + increasedQuantity) * productLocation.costAmount.get
      record.reason ==== reason
      record.notes ==== notes
    }

    def assertNoProductHistoryRecord(productId: UUID, locationId: UUID) = {
      val records = productQuantityHistoryDao.findAllByProductIdsAndLocationId(Seq(productId), locationId).await
      records ==== Seq.empty
    }
  }

  trait ProductStockFixtures { self: ServiceDaoSpecContext =>
    val jeans = Factory.simpleProduct(merchant, trackInventory = Some(true), trackInventoryParts = Some(false)).create
    val jeansLondon = Factory.productLocation(jeans, london, costAmount = Some(10)).create
    val jeansLondonStock = Factory.stock(jeansLondon, quantity = Some(100)).create

    val shirt = Factory.simpleProduct(merchant, trackInventory = Some(false), trackInventoryParts = Some(true)).create
    val shirtLondon = Factory.productLocation(shirt, london, costAmount = Some(1)).create
    val shirtLondonStock = Factory.stock(shirtLondon, quantity = Some(50)).create

    val recipe = Factory.simplePart(merchant, trackInventory = Some(true), trackInventoryParts = Some(true)).create
    val recipeLondon = Factory.productLocation(recipe, london, costAmount = Some(0.5)).create
    val recipeLondonStock = Factory.stock(recipeLondon, quantity = Some(5000)).create

    val ingredient = Factory.simplePart(merchant, trackInventory = Some(true), trackInventoryParts = Some(true)).create
    val ingredientLondon = Factory.productLocation(ingredient, london, costAmount = Some(0.1)).create
    val ingredientLondonStock = Factory.stock(ingredientLondon, quantity = Some(1000)).create

    Factory.productPart(recipe, ingredient, quantityNeeded = Some(3)).create

    Factory.productPart(jeans, ingredient, quantityNeeded = Some(20)).create
    Factory.productPart(jeans, recipe, quantityNeeded = Some(2)).create

    Factory.productPart(shirt, ingredient, quantityNeeded = Some(100)).create
    Factory.productPart(shirt, recipe, quantityNeeded = Some(50)).create

    lazy val bundle = Factory.comboProduct(merchant).create
    lazy val bundleSet = Factory.bundleSet(bundle).create
    lazy val bundleOption1 = Factory.bundleOption(bundleSet, jeans).create
    lazy val bundleOption2 = Factory.bundleOption(bundleSet, shirt).create
  }

}
