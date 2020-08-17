package io.paytouch.core.resources.returnorders

import java.time.LocalDateTime

import io.paytouch.core.data.model.enums.ReturnOrderStatus
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ PaginatedApiResponse, ReturnOrder => ReturnOrderEntity }
import io.paytouch.core.utils.{ Formatters, UtcTime, FixtureDaoFactory => Factory }

class ReturnOrdersListFSpec extends ReturnOrdersFSpec {

  trait ReturnOrdersListFSpecContext extends ReturnOrderResourceFSpecContext {
    val now = UtcTime.ofLocalDateTime(LocalDateTime.parse("2017-06-19T16:32:31"))
    val yesterday = now.minusDays(1)
    val yesterdayAsString = Formatters.LocalDateFormatter.format(yesterday.toLocalDate)

    val supplier = Factory.supplier(merchant).create
  }

  "GET /v1/return_orders.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all return orders" in new ReturnOrdersListFSpecContext {
          val returnOrder1 = Factory.returnOrder(user, supplier, rome).create
          val returnOrder2 = Factory.returnOrder(user, supplier, rome).create
          val returnOrder3 = Factory.returnOrder(user, supplier, rome).create
          val returnOrder4 = Factory.returnOrder(user, supplier, rome).create

          Get("/v1/return_orders.list").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReturnOrderEntity]]]
            entities.data.map(_.id) ==== Seq(returnOrder1.id, returnOrder2.id, returnOrder3.id, returnOrder4.id)
            assertResponse(entities.data.find(_.id == returnOrder1.id).get, returnOrder1)
            assertResponse(entities.data.find(_.id == returnOrder2.id).get, returnOrder2)
            assertResponse(entities.data.find(_.id == returnOrder3.id).get, returnOrder3)
            assertResponse(entities.data.find(_.id == returnOrder4.id).get, returnOrder4)
          }
        }
      }

      "with location_id filter" should {
        "return a paginated list of all return orders filtered by location_id (applied on from_location)" in new ReturnOrdersListFSpecContext {
          val returnOrder1 = Factory.returnOrder(user, supplier, rome).create
          val returnOrder2 = Factory.returnOrder(user, supplier, rome).create
          val returnOrder3 = Factory.returnOrder(user, supplier, london).create
          val returnOrder4 = Factory.returnOrder(user, supplier, london).create

          Get(s"/v1/return_orders.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReturnOrderEntity]]]
            entities.data.map(_.id) ==== Seq(returnOrder1.id, returnOrder2.id)
            assertResponse(entities.data.find(_.id == returnOrder1.id).get, returnOrder1)
            assertResponse(entities.data.find(_.id == returnOrder2.id).get, returnOrder2)
          }
        }
      }

      "with from filter" should {
        "return a paginated list of all return orders filtered by from date" in new ReturnOrdersListFSpecContext {
          val returnOrder1 =
            Factory.returnOrder(user, supplier, rome, overrideNow = Some(yesterday.minusDays(1))).createAndReload

          val returnOrder2 =
            Factory.returnOrder(user, supplier, rome, overrideNow = Some(yesterday.plusDays(1))).createAndReload

          Get(s"/v1/return_orders.list?from=$yesterdayAsString").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReturnOrderEntity]]]
            entities.data.map(_.id) ==== Seq(returnOrder2.id)
            assertResponse(entities.data.find(_.id == returnOrder2.id).get, returnOrder2)
          }
        }
      }

      "with to filter" should {
        "return a paginated list of all return orders filtered by to date" in new ReturnOrdersListFSpecContext {
          val returnOrder1 =
            Factory.returnOrder(user, supplier, rome, overrideNow = Some(yesterday.minusDays(1))).createAndReload
          val returnOrder2 =
            Factory.returnOrder(user, supplier, rome, overrideNow = Some(yesterday.plusDays(1))).createAndReload

          Get(s"/v1/return_orders.list?to=$yesterdayAsString").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReturnOrderEntity]]]
            entities.data.map(_.id) ==== Seq(returnOrder1.id)
            assertResponse(entities.data.find(_.id == returnOrder1.id).get, returnOrder1)
          }
        }
      }

      "with q filter" should {
        "return a paginated list of all return orders filtered by a given query on return order number" in new ReturnOrdersListFSpecContext {
          val returnOrder1 = Factory.returnOrder(user, supplier, rome).create
          val returnOrder2 = Factory.returnOrder(user, supplier, rome).create
          val returnOrder3 = Factory.returnOrder(user, supplier, rome).create
          val returnOrder4 = Factory.returnOrder(user, supplier, rome).create

          Get("/v1/return_orders.list?q=3").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReturnOrderEntity]]]
            entities.data.map(_.id) ==== Seq(returnOrder3.id)
            assertResponse(entities.data.find(_.id == returnOrder3.id).get, returnOrder3)
          }
        }
      }

      "with no parameter" should {
        "return a paginated list of all return orders" in new ReturnOrdersListFSpecContext {
          val returnOrder1 = Factory.returnOrder(user, supplier, rome, status = Some(ReturnOrderStatus.Accepted)).create
          val returnOrder2 = Factory.returnOrder(user, supplier, rome, status = Some(ReturnOrderStatus.Canceled)).create
          val returnOrder3 = Factory.returnOrder(user, supplier, rome, status = Some(ReturnOrderStatus.Accepted)).create

          Get("/v1/return_orders.list?status=accepted").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReturnOrderEntity]]]
            entities.data.map(_.id) ==== Seq(returnOrder1.id, returnOrder3.id)
            assertResponse(entities.data.find(_.id == returnOrder1.id).get, returnOrder1)
            assertResponse(entities.data.find(_.id == returnOrder3.id).get, returnOrder3)
          }
        }
      }

      "with expand[]=location" should {
        "return a return order" in new ReturnOrdersListFSpecContext {
          val returnOrder = Factory.returnOrder(user, supplier, rome).create

          Get(s"/v1/return_orders.list?expand[]=location").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReturnOrderEntity]]]
            assertResponse(entities.data.find(_.id == returnOrder.id).get, returnOrder, location = Some(rome))
          }
        }
      }

      "with expand[]=user" should {
        "return a return order" in new ReturnOrdersListFSpecContext {
          val returnOrder = Factory.returnOrder(user, supplier, rome).create

          Get(s"/v1/return_orders.list?expand[]=user").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReturnOrderEntity]]]
            assertResponse(entities.data.find(_.id == returnOrder.id).get, returnOrder, user = Some(user))
          }
        }
      }

      "with expand[]=supplier" should {
        "return a return order" in new ReturnOrdersListFSpecContext {
          val returnOrder = Factory.returnOrder(user, supplier, rome).create

          Get(s"/v1/return_orders.list?expand[]=supplier").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReturnOrderEntity]]]
            assertResponse(entities.data.find(_.id == returnOrder.id).get, returnOrder, supplier = Some(supplier))
          }
        }
      }

      "with expand[]=stock_value" should {
        "return a return order" in new ReturnOrdersListFSpecContext {
          val returnOrder = Factory.returnOrder(user, supplier, rome).create

          val product = Factory.simpleProduct(merchant).create
          Factory.productLocation(product, rome, costAmount = Some(2)).create

          val returnOrderProduct = Factory.returnOrderProduct(returnOrder, product, quantity = Some(3)).create

          Get(s"/v1/return_orders.list?expand[]=stock_value").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReturnOrderEntity]]]
            assertResponse(entities.data.find(_.id == returnOrder.id).get, returnOrder, stockValue = Some(6.$$$))
          }
        }
      }

      "with expand[]=products_count" should {
        "return a return order" in new ReturnOrdersListFSpecContext {
          val returnOrder = Factory.returnOrder(user, supplier, rome).create

          val product1 = Factory.simpleProduct(merchant).create
          val product2 = Factory.simpleProduct(merchant).create

          val returnOrderProduct1 = Factory.returnOrderProduct(returnOrder, product1, quantity = Some(3)).create
          val returnOrderProduct2 = Factory.returnOrderProduct(returnOrder, product2, quantity = Some(4)).create

          Get(s"/v1/return_orders.list?expand[]=products_count").addHeader(authorizationHeader) ~> routes ~> check {
            val entities = responseAs[PaginatedApiResponse[Seq[ReturnOrderEntity]]]
            assertResponse(entities.data.find(_.id == returnOrder.id).get, returnOrder, productsCount = Some(7))
          }
        }
      }
    }
  }

}
