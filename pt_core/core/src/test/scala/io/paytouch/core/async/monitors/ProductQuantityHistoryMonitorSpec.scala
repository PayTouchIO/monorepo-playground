package io.paytouch.core.async.monitors

import akka.actor.Props
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ProductQuantityHistoryMonitorSpec extends MonitorSpec {

  abstract class ProductQuantityHistoryMonitorSpecContext extends MonitorSpecContext with StateFixtures {

    lazy val monitor = monitorSystem.actorOf(Props(new ProductQuantityHistoryMonitor))

    val productQuantityHistoryDao = daos.productQuantityHistoryDao
  }

  "ProductQuantityHistoryMonitor" in {

    "record a quantity change" in new ProductQuantityHistoryMonitorSpecContext {
      val prevQuantity: BigDecimal = 10
      val newQuantity: BigDecimal = 25

      monitor ! baseIncrease.copy(prevQuantity = prevQuantity, newQuantity = newQuantity)

      afterAWhile {
        val maybeRecord =
          productQuantityHistoryDao.findAllByProductIdsAndLocationId(Seq(product.id), london.id).await.headOption
        maybeRecord should beSome
        val record = maybeRecord.get

        record.merchantId ==== merchant.id
        record.productId ==== product.id
        record.locationId ==== london.id
        record.userId ==== Some(user.id)
        record.orderId ==== Some(order.id)
        record.date should not(beNull)
        record.prevQuantityAmount ==== prevQuantity
        record.newQuantityAmount ==== newQuantity
        record.newStockValueAmount ==== 2 * newQuantity
        record.reason ==== baseIncrease.reason
        record.notes ==== baseIncrease.notes
      }
    }

    "do nothing if there is no quantity change" in new ProductQuantityHistoryMonitorSpecContext {
      val quantity: BigDecimal = 10

      monitor ! baseIncrease.copy(prevQuantity = quantity, newQuantity = quantity)

      // giving some time to process message
      Thread.sleep(100)

      afterAWhile {
        val maybeRecord =
          productQuantityHistoryDao.findAllByProductIdsAndLocationId(Seq(product.id), london.id).await.headOption
        maybeRecord should beNone
      }
    }
  }

  trait StateFixtures extends MonitorStateFixtures {
    val product = Factory.simpleProduct(merchant).create
    val productLocation = Factory.productLocation(product, london, costAmount = Some(2)).create
    val order = Factory.order(merchant).create

    @scala.annotation.nowarn("msg=Auto-application")
    val baseIncrease = random[ProductQuantityIncrease].copy(
      productLocation = productLocation,
      orderId = Some(order.id),
      userContext = userContext,
    )
  }
}
