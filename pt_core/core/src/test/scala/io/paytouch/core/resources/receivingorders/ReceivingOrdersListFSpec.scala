package io.paytouch.core.resources.receivingorders

import io.paytouch.core.data.model.enums.ReceivingOrderStatus
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ PaginatedApiResponse, ReceivingOrder => ReceivingOrderEntity }
import io.paytouch.core.utils.{ Formatters, UtcTime, FixtureDaoFactory => Factory }

class ReceivingOrdersListFSpec extends ReceivingOrdersFSpec {

  trait ReceivingOrdersListFspecContext extends ReceivingOrderResourceFSpecContext

  "GET /v1/receiving_orders.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all receiving orders" in new ReceivingOrdersListFspecContext {
          val receivingOrder1 = Factory.receivingOrder(rome, user).create
          val receivingOrder2 = Factory.receivingOrder(rome, user).create
          val receivingOrder3 = Factory.receivingOrder(rome, user).create
          val receivingOrder4 = Factory.receivingOrder(rome, user).create

          Get("/v1/receiving_orders.list").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReceivingOrderEntity]]]
            entities.data.map(_.id) ==== Seq(
              receivingOrder1.id,
              receivingOrder2.id,
              receivingOrder3.id,
              receivingOrder4.id,
            )
            assertResponse(entities.data.find(_.id == receivingOrder1.id).get, receivingOrder1)
            assertResponse(entities.data.find(_.id == receivingOrder2.id).get, receivingOrder2)
            assertResponse(entities.data.find(_.id == receivingOrder3.id).get, receivingOrder3)
            assertResponse(entities.data.find(_.id == receivingOrder4.id).get, receivingOrder4)
          }
        }
      }

      "with location_id filter" should {
        "return a paginated list of all receiving orders filtered by location_id" in new ReceivingOrdersListFspecContext {
          val receivingOrder1 = Factory.receivingOrder(rome, user).create
          val receivingOrder2 = Factory.receivingOrder(rome, user).create
          val receivingOrder3 = Factory.receivingOrder(london, user).create
          val receivingOrder4 = Factory.receivingOrder(london, user).create

          Get(s"/v1/receiving_orders.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReceivingOrderEntity]]]
            entities.data.map(_.id) ==== Seq(receivingOrder1.id, receivingOrder2.id)
            assertResponse(entities.data.find(_.id == receivingOrder1.id).get, receivingOrder1)
            assertResponse(entities.data.find(_.id == receivingOrder2.id).get, receivingOrder2)
          }
        }
      }

      "with from filter" should {
        "return a paginated list of all receiving orders filtered by from date" in new ReceivingOrdersListFspecContext {
          val yesterday = UtcTime.now.minusDays(1)
          val yesterdayAsString = Formatters.LocalDateTimeFormatter.format(yesterday)

          val receivingOrder1 =
            Factory.receivingOrder(rome, user, overrideNow = Some(yesterday.minusDays(10))).createAndReload
          val receivingOrder2 =
            Factory.receivingOrder(rome, user, overrideNow = Some(yesterday.plusDays(10))).createAndReload

          Get(s"/v1/receiving_orders.list?from=$yesterdayAsString").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReceivingOrderEntity]]]
            entities.data.map(_.id) ==== Seq(receivingOrder2.id)
            assertResponse(entities.data.find(_.id == receivingOrder2.id).get, receivingOrder2)
          }
        }
      }

      "with to filter" should {
        "return a paginated list of all receiving orders filtered by to date" in new ReceivingOrdersListFspecContext {
          val yesterday = UtcTime.now.minusDays(1)
          val yesterdayAsString = Formatters.LocalDateTimeFormatter.format(yesterday)

          val receivingOrder1 =
            Factory.receivingOrder(rome, user, overrideNow = Some(yesterday.minusDays(10))).createAndReload
          val receivingOrder2 =
            Factory.receivingOrder(rome, user, overrideNow = Some(yesterday.plusDays(10))).createAndReload

          Get(s"/v1/receiving_orders.list?to=$yesterdayAsString").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReceivingOrderEntity]]]
            entities.data.map(_.id) ==== Seq(receivingOrder1.id)
            assertResponse(entities.data.find(_.id == receivingOrder1.id).get, receivingOrder1)
          }
        }
      }

      "with q filter" should {
        "return a paginated list of all receiving orders filtered by a given query on receiving order number" in new ReceivingOrdersListFspecContext {
          val yesterday = UtcTime.now.minusDays(1)
          val yesterdayAsString = Formatters.LocalDateTimeFormatter.format(yesterday)

          val receivingOrder1 = Factory.receivingOrder(rome, user).create
          val receivingOrder2 = Factory.receivingOrder(rome, user).create

          Get(s"/v1/receiving_orders.list?q=2").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReceivingOrderEntity]]]
            entities.data.map(_.id) ==== Seq(receivingOrder2.id)
            assertResponse(entities.data.find(_.id == receivingOrder2.id).get, receivingOrder2)
          }
        }
      }

      "with status filter" should {
        "return a paginated list of all receiving orders filtered by status" in new ReceivingOrdersListFspecContext {
          val yesterday = UtcTime.now.minusDays(1)
          val yesterdayAsString = Formatters.LocalDateTimeFormatter.format(yesterday)

          val receivingOrder1 =
            Factory.receivingOrder(rome, user, status = Some(ReceivingOrderStatus.Receiving)).create
          val receivingOrder2 = Factory.receivingOrder(rome, user, status = Some(ReceivingOrderStatus.Received)).create

          Get(s"/v1/receiving_orders.list?status=received").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReceivingOrderEntity]]]
            entities.data.map(_.id) ==== Seq(receivingOrder2.id)
            assertResponse(entities.data.find(_.id == receivingOrder2.id).get, receivingOrder2)
          }
        }
      }

      "with expand[]=products_count" should {
        "return a paginated list of all receiving orders with products count expanded" in new ReceivingOrdersListFspecContext {
          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant).create

          val receivingOrder1 = Factory.receivingOrder(rome, user).create
          val receivingOrder1Product1 =
            Factory.receivingOrderProduct(receivingOrder1, product1, quantity = Some(3)).create
          val receivingOrder1Product2 =
            Factory.receivingOrderProduct(receivingOrder1, product2, quantity = Some(1)).create

          val receivingOrder2 = Factory.receivingOrder(rome, user).create

          Get(s"/v1/receiving_orders.list?expand[]=products_count").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReceivingOrderEntity]]]
            entities.data.map(_.id) ==== Seq(receivingOrder1.id, receivingOrder2.id)
            assertResponse(entities.data.find(_.id == receivingOrder1.id).get, receivingOrder1, productsCount = Some(4))
            assertResponse(entities.data.find(_.id == receivingOrder2.id).get, receivingOrder2, productsCount = Some(0))
          }
        }
      }

      "with expand[]=stock_value" should {
        "return a paginated list of all receiving orders with stock value expanded" in new ReceivingOrdersListFspecContext {
          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant).create

          val receivingOrder1 = Factory.receivingOrder(rome, user).create
          val receivingOrder1Product1 =
            Factory
              .receivingOrderProduct(receivingOrder1, product1, quantity = Some(3), costAmount = Some(2))
              .create
          val receivingOrder1Product2 =
            Factory
              .receivingOrderProduct(receivingOrder1, product2, quantity = Some(1), costAmount = Some(3))
              .create

          val receivingOrder2 = Factory.receivingOrder(rome, user).create

          Get(s"/v1/receiving_orders.list?expand[]=stock_value").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReceivingOrderEntity]]]
            entities.data.map(_.id) ==== Seq(receivingOrder1.id, receivingOrder2.id)
            assertResponse(
              entities.data.find(_.id == receivingOrder1.id).get,
              receivingOrder1,
              stockValue = Some(9.$$$),
            )
            assertResponse(entities.data.find(_.id == receivingOrder2.id).get, receivingOrder2, stockValue = None)
          }
        }
      }

      "with expand[]=user" should {
        "return a paginated list of all receiving orders with user expanded" in new ReceivingOrdersListFspecContext {
          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant).create

          val receivingOrder1 = Factory.receivingOrder(rome, user).create
          val receivingOrder1Product1 =
            Factory
              .receivingOrderProduct(receivingOrder1, product1, quantity = Some(3), costAmount = Some(2))
              .create
          val receivingOrder1Product2 =
            Factory
              .receivingOrderProduct(receivingOrder1, product2, quantity = Some(1), costAmount = Some(3))
              .create

          val receivingOrder2 = Factory.receivingOrder(rome, user).create

          Get(s"/v1/receiving_orders.list?expand[]=user").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReceivingOrderEntity]]]
            entities.data.map(_.id) ==== Seq(receivingOrder1.id, receivingOrder2.id)
            assertResponse(entities.data.find(_.id == receivingOrder1.id).get, receivingOrder1, user = Some(user))
            assertResponse(entities.data.find(_.id == receivingOrder2.id).get, receivingOrder2, user = Some(user))
          }
        }
      }
    }
  }

}
