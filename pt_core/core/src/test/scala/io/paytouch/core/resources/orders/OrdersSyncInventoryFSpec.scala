package io.paytouch.core.resources.orders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities.{ Order => OrderEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class OrdersSyncInventoryFSpec extends OrdersSyncAssertsFSpec {
  class OrdersSyncInventoryContext extends OrdersSyncFSpecContext with Fixtures {
    val product1 = Factory.simpleProduct(merchant, trackInventory = Some(true)).create
    val product1London = Factory.productLocation(product1, london).create
    val product1LondonStock = Factory.stock(product1London, quantity = Some(100)).create

    val product2 = Factory.simpleProduct(merchant, trackInventory = Some(false)).create
    val product2London = Factory.productLocation(product2, london).create
    val product2LondonStock = Factory.stock(product2London, quantity = Some(50)).create
  }

  "POST /v1/orders.sync?order_id=$" in {
    "new order" should {
      class Context extends OrdersSyncInventoryContext {
        val orderId = UUID.randomUUID
      }

      "simple products" should {
        "decreases stock for products with inventory tracking enabled" in new Context {
          val orderItem1Upsertion = randomOrderItemUpsertion().copy(
            productId = Some(product1.id),
            quantity = Some(2),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val orderItem2Upsertion = randomOrderItemUpsertion().copy(
            productId = Some(product2.id),
            quantity = Some(20),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val upsertion = baseOrderUpsertion.copy(
            status = OrderStatus.InProgress,
            items = Seq(orderItem1Upsertion, orderItem2Upsertion),
          )

          Post(s"/v1/orders.sync?order_id=${orderId}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)

            afterAWhile {
              val ids = Seq(product1LondonStock.id, product2LondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == product1LondonStock.id).get.quantity ==== product1LondonStock.quantity - 2
              stocks.find(_.id == product2LondonStock.id).get.quantity ==== product2LondonStock.quantity
            }
          }
        }
      }
    }

    "existing order" should {
      class Context extends OrdersSyncInventoryContext {
        val order = Factory
          .order(merchant, paymentStatus = Some(PaymentStatus.Pending), status = Some(OrderStatus.InProgress))
          .create
        val orderItem1 = Factory.orderItem(order, product = Some(product1), quantity = Some(2)).create
        val orderItem2 = Factory.orderItem(order, product = Some(product2), quantity = Some(20)).create
      }

      "simple products" should {
        "doesn't change stock where quantity isn't changed" in new Context {
          val orderItem1Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem1.id,
            productId = Some(product1.id),
            quantity = Some(2),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val orderItem2Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem2.id,
            productId = Some(product2.id),
            quantity = Some(20),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val upsertion = baseOrderUpsertion.copy(
            status = OrderStatus.InProgress,
            items = Seq(orderItem1Upsertion, orderItem2Upsertion),
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)

            afterAWhile {
              val ids = Seq(product1LondonStock.id, product2LondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == product1LondonStock.id).get.quantity ==== product1LondonStock.quantity
              stocks.find(_.id == product2LondonStock.id).get.quantity ==== product2LondonStock.quantity
            }
          }
        }

        "decreases stock where quantity is increased" in new Context {
          val orderItem1Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem1.id,
            productId = Some(product1.id),
            quantity = Some(3),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val orderItem2Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem2.id,
            productId = Some(product2.id),
            quantity = Some(30),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val upsertion = baseOrderUpsertion.copy(
            status = OrderStatus.InProgress,
            items = Seq(orderItem1Upsertion, orderItem2Upsertion),
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)

            afterAWhile {
              val ids = Seq(product1LondonStock.id, product2LondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == product1LondonStock.id).get.quantity ==== product1LondonStock.quantity - 1
              stocks.find(_.id == product2LondonStock.id).get.quantity ==== product2LondonStock.quantity
            }
          }
        }

        "increases stock where quantity is decreased" in new Context {
          val orderItem1Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem1.id,
            productId = Some(product1.id),
            quantity = Some(1),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val orderItem2Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem2.id,
            productId = Some(product2.id),
            quantity = Some(10),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val upsertion = baseOrderUpsertion.copy(
            status = OrderStatus.InProgress,
            items = Seq(orderItem1Upsertion, orderItem2Upsertion),
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)

            afterAWhile {
              val ids = Seq(product1LondonStock.id, product2LondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == product1LondonStock.id).get.quantity ==== product1LondonStock.quantity + 1
              stocks.find(_.id == product2LondonStock.id).get.quantity ==== product2LondonStock.quantity
            }
          }
        }

        "releases stock when an item is refunded" in new Context {
          val orderItem1Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem1.id,
            productId = Some(product1.id),
            quantity = Some(2),
            paymentStatus = Some(PaymentStatus.Refunded),
          )

          val orderItem2Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem2.id,
            productId = Some(product2.id),
            quantity = Some(20),
            paymentStatus = Some(PaymentStatus.Refunded),
          )

          val upsertion = baseOrderUpsertion.copy(
            status = OrderStatus.InProgress,
            items = Seq(orderItem1Upsertion, orderItem2Upsertion),
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)

            afterAWhile {
              val ids = Seq(product1LondonStock.id, product2LondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == product1LondonStock.id).get.quantity ==== product1LondonStock.quantity + 2
              stocks.find(_.id == product2LondonStock.id).get.quantity ==== product2LondonStock.quantity
            }
          }
        }

        "releases stock when an item is deleted" in new Context {
          val orderItem1Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem1.id,
            productId = Some(product1.id),
            quantity = Some(2),
            paymentStatus = Some(PaymentStatus.Voided),
          )

          val orderItem2Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem2.id,
            productId = Some(product2.id),
            quantity = Some(20),
            paymentStatus = Some(PaymentStatus.Voided),
          )

          val upsertion = baseOrderUpsertion.copy(
            status = OrderStatus.InProgress,
            items = Seq.empty,
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)

            afterAWhile {
              val ids = Seq(product1LondonStock.id, product2LondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == product1LondonStock.id).get.quantity ==== product1LondonStock.quantity + 2
              stocks.find(_.id == product2LondonStock.id).get.quantity ==== product2LondonStock.quantity
            }
          }
        }

        "releases stock when an order is cancelled" in new Context {
          val orderItem1Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem1.id,
            productId = Some(product1.id),
            quantity = Some(2),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val orderItem2Upsertion = randomOrderItemUpsertion().copy(
            id = orderItem2.id,
            productId = Some(product2.id),
            quantity = Some(20),
            paymentStatus = Some(PaymentStatus.Pending),
          )

          val upsertion = baseOrderUpsertion.copy(
            status = OrderStatus.Canceled,
            items = Seq(orderItem1Upsertion, orderItem2Upsertion),
          )

          Post(s"/v1/orders.sync?order_id=${order.id}", upsertion).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderResponse = responseAs[ApiResponse[OrderEntity]].data
            assertUpsertion(orderResponse, upsertion)

            afterAWhile {
              val ids = Seq(product1LondonStock.id, product2LondonStock.id)
              val stocks = stockDao.findByIds(ids).await
              stocks.find(_.id == product1LondonStock.id).get.quantity ==== product1LondonStock.quantity + 2
              stocks.find(_.id == product2LondonStock.id).get.quantity ==== product2LondonStock.quantity
            }
          }
        }
      }
    }
  }
}
