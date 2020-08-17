package io.paytouch.core.resources.orders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.{ Order => OrderEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.core.USD

class OrdersSyncBundlesFSpec extends OrdersSyncAssertsFSpec {
  class OrderSyncBundlesContext extends OrdersSyncFSpecContext with Fixtures {
    val product1 = Factory.simpleProduct(merchant, costAmount = Some(5)).create
    val product2 = Factory.simpleProduct(merchant, costAmount = Some(7)).create

    val bundle1 = Factory.comboProduct(merchant).create
    val bundleSet1 = Factory.bundleSet(bundle1).create
    val bundleOption1 = Factory.bundleOption(bundleSet1, product1).create
    val bundleSet2 = Factory.bundleSet(bundle1).create
    val bundleOption2 = Factory.bundleOption(bundleSet2, product2).create
  }

  "POST /v1/orders.sync?order_id=$" in {
    "new order" should {
      class Context extends OrderSyncBundlesContext {
        val orderId = UUID.randomUUID
      }

      "calculate the cost for the bundle with single quantities" in new Context {
        // Bundle option 1

        val orderItem1Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(product1.id),
          quantity = Some(1),
          costAmount = product1.costAmount,
        )

        val orderBundleOption1Upsertion = random[OrderBundleOptionUpsertion].copy(
          id = UUID.randomUUID,
          bundleOptionId = bundleOption1.id,
          articleOrderItemId = orderItem1Upsertion.id,
        )

        val orderBundleSet1Upsertion = random[OrderBundleSetUpsertion].copy(
          id = UUID.randomUUID,
          bundleSetId = bundleSet1.id,
          orderBundleOptions = Seq(orderBundleOption1Upsertion),
        )

        // Bundle option 2

        val orderItem2Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(product2.id),
          quantity = Some(1),
          costAmount = product2.costAmount,
        )

        val orderBundleOption2Upsertion = random[OrderBundleOptionUpsertion].copy(
          id = UUID.randomUUID,
          bundleOptionId = bundleOption2.id,
          articleOrderItemId = orderItem2Upsertion.id,
        )

        val orderBundleSet2Upsertion = random[OrderBundleSetUpsertion].copy(
          id = UUID.randomUUID,
          bundleSetId = bundleSet2.id,
          orderBundleOptions = Seq(orderBundleOption2Upsertion),
        )

        // Bundle

        val orderItem3Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(bundle1.id),
          quantity = Some(1),
          costAmount = Some(0),
        )

        val orderBundleUpsertion = random[OrderBundleUpsertion].copy(
          id = UUID.randomUUID,
          bundleOrderItemId = orderItem3Upsertion.id,
          orderBundleSets = Seq(orderBundleSet1Upsertion, orderBundleSet2Upsertion),
        )

        val upsertion = baseOrderUpsertion.copy(
          items = Seq(orderItem1Upsertion, orderItem2Upsertion, orderItem3Upsertion),
          bundles = Seq(orderBundleUpsertion),
        )

        Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val orderResponse = responseAs[ApiResponse[OrderEntity]].data
          assertUpsertion(orderResponse, upsertion)

          val bundleItemResponse = orderResponse.items.getOrElse(Seq.empty).find(_.productId == Some(bundle1.id)).get
          bundleItemResponse.cost.get.amount ==== 5 + 7
        }
      }

      "calculate the cost for the bundle without item costs" in new Context {
        // Bundle option 1

        val orderItem1Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(product1.id),
          quantity = Some(1),
          costAmount = None,
        )

        val orderBundleOption1Upsertion = random[OrderBundleOptionUpsertion].copy(
          id = UUID.randomUUID,
          bundleOptionId = bundleOption1.id,
          articleOrderItemId = orderItem1Upsertion.id,
        )

        val orderBundleSet1Upsertion = random[OrderBundleSetUpsertion].copy(
          id = UUID.randomUUID,
          bundleSetId = bundleSet1.id,
          orderBundleOptions = Seq(orderBundleOption1Upsertion),
        )

        // Bundle option 2

        val orderItem2Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(product2.id),
          quantity = Some(1),
          costAmount = None,
        )

        val orderBundleOption2Upsertion = random[OrderBundleOptionUpsertion].copy(
          id = UUID.randomUUID,
          bundleOptionId = bundleOption2.id,
          articleOrderItemId = orderItem2Upsertion.id,
        )

        val orderBundleSet2Upsertion = random[OrderBundleSetUpsertion].copy(
          id = UUID.randomUUID,
          bundleSetId = bundleSet2.id,
          orderBundleOptions = Seq(orderBundleOption2Upsertion),
        )

        // Bundle

        val orderItem3Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(bundle1.id),
          quantity = Some(1),
          costAmount = None,
        )

        val orderBundleUpsertion = random[OrderBundleUpsertion].copy(
          id = UUID.randomUUID,
          bundleOrderItemId = orderItem3Upsertion.id,
          orderBundleSets = Seq(orderBundleSet1Upsertion, orderBundleSet2Upsertion),
        )

        val upsertion = baseOrderUpsertion.copy(
          items = Seq(orderItem1Upsertion, orderItem2Upsertion, orderItem3Upsertion),
          bundles = Seq(orderBundleUpsertion),
        )

        Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val orderResponse = responseAs[ApiResponse[OrderEntity]].data
          assertUpsertion(orderResponse, upsertion)

          val bundleItemResponse = orderResponse.items.getOrElse(Seq.empty).find(_.productId == Some(bundle1.id)).get
          bundleItemResponse.cost.get.amount ==== 5 + 7
        }
      }

      "calculate the cost for the bundle with multiple quantities" in new Context {
        // Bundle option 1

        val orderItem1Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(product1.id),
          quantity = Some(2),
          costAmount = product1.costAmount,
        )

        val orderBundleOption1Upsertion = random[OrderBundleOptionUpsertion].copy(
          id = UUID.randomUUID,
          bundleOptionId = bundleOption1.id,
          articleOrderItemId = orderItem1Upsertion.id,
        )

        val orderBundleSet1Upsertion = random[OrderBundleSetUpsertion].copy(
          id = UUID.randomUUID,
          bundleSetId = bundleSet1.id,
          orderBundleOptions = Seq(orderBundleOption1Upsertion),
        )

        // Bundle option 2

        val orderItem2Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(product2.id),
          quantity = Some(2),
          costAmount = product2.costAmount,
        )

        val orderBundleOption2Upsertion = random[OrderBundleOptionUpsertion].copy(
          id = UUID.randomUUID,
          bundleOptionId = bundleOption2.id,
          articleOrderItemId = orderItem2Upsertion.id,
        )

        val orderBundleSet2Upsertion = random[OrderBundleSetUpsertion].copy(
          id = UUID.randomUUID,
          bundleSetId = bundleSet2.id,
          orderBundleOptions = Seq(orderBundleOption2Upsertion),
        )

        // Bundle

        val orderItem3Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(bundle1.id),
          quantity = Some(3),
          costAmount = Some(0),
        )

        val orderBundleUpsertion = random[OrderBundleUpsertion].copy(
          id = UUID.randomUUID,
          bundleOrderItemId = orderItem3Upsertion.id,
          orderBundleSets = Seq(orderBundleSet1Upsertion, orderBundleSet2Upsertion),
        )

        val upsertion = baseOrderUpsertion.copy(
          items = Seq(orderItem1Upsertion, orderItem2Upsertion, orderItem3Upsertion),
          bundles = Seq(orderBundleUpsertion),
        )

        Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val orderResponse = responseAs[ApiResponse[OrderEntity]].data
          assertUpsertion(orderResponse, upsertion)

          val bundleItemResponse = orderResponse.items.getOrElse(Seq.empty).find(_.productId == Some(bundle1.id)).get
          bundleItemResponse.cost.get.amount ==== (5 * 2) + (7 * 2)
        }
      }

      "calculate the cost for the bundle with custom bundle cost" in new Context {
        // Bundle option 1

        val orderItem1Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(product1.id),
          quantity = Some(1),
          costAmount = None,
        )

        val orderBundleOption1Upsertion = random[OrderBundleOptionUpsertion].copy(
          id = UUID.randomUUID,
          bundleOptionId = bundleOption1.id,
          articleOrderItemId = orderItem1Upsertion.id,
        )

        val orderBundleSet1Upsertion = random[OrderBundleSetUpsertion].copy(
          id = UUID.randomUUID,
          bundleSetId = bundleSet1.id,
          orderBundleOptions = Seq(orderBundleOption1Upsertion),
        )

        // Bundle option 2

        val orderItem2Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(product2.id),
          quantity = Some(1),
          costAmount = None,
        )

        val orderBundleOption2Upsertion = random[OrderBundleOptionUpsertion].copy(
          id = UUID.randomUUID,
          bundleOptionId = bundleOption2.id,
          articleOrderItemId = orderItem2Upsertion.id,
        )

        val orderBundleSet2Upsertion = random[OrderBundleSetUpsertion].copy(
          id = UUID.randomUUID,
          bundleSetId = bundleSet2.id,
          orderBundleOptions = Seq(orderBundleOption2Upsertion),
        )

        // Bundle

        val orderItem3Upsertion = randomOrderItemUpsertion().copy(
          productId = Some(bundle1.id),
          quantity = Some(1),
          costAmount = Some(18),
        )

        val orderBundleUpsertion = random[OrderBundleUpsertion].copy(
          id = UUID.randomUUID,
          bundleOrderItemId = orderItem3Upsertion.id,
          orderBundleSets = Seq(orderBundleSet1Upsertion, orderBundleSet2Upsertion),
        )

        val upsertion = baseOrderUpsertion.copy(
          items = Seq(orderItem1Upsertion, orderItem2Upsertion, orderItem3Upsertion),
          bundles = Seq(orderBundleUpsertion),
        )

        Post(s"/v1/orders.sync?order_id=$orderId", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()
          val orderResponse = responseAs[ApiResponse[OrderEntity]].data
          assertUpsertion(orderResponse, upsertion)

          val bundleItemResponse = orderResponse.items.getOrElse(Seq.empty).find(_.productId == Some(bundle1.id)).get
          bundleItemResponse.cost.get.amount ==== 18
        }
      }
    }
  }
}
