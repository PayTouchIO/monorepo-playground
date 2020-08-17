package io.paytouch.core.resources.orderfeedback

import java.time.ZonedDateTime

import io.paytouch.core.entities.{ OrderFeedback, PaginatedApiResponse }
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class OrderFeedbackListFSpec extends OrderFeedbackFSpec {

  "GET /v1/order_feedback.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all order feedback sorted by read (unread first) and date" in new OrderFeedbackResourceFSpecContext {
          val newYork = Factory.location(merchant).create
          val customer = Factory.globalCustomer(Some(merchant)).create
          val orderRome = Factory.order(merchant, Some(rome)).create
          val orderNewYork = Factory.order(merchant, Some(newYork)).create

          val orderFeedback1 = Factory
            .orderFeedback(orderRome, customer, read = Some(true), receivedAt = Some(UtcTime.now.minusDays(1)))
            .create
          val orderFeedback2 = Factory
            .orderFeedback(orderRome, customer, read = Some(false), receivedAt = Some(UtcTime.now.minusDays(2)))
            .create
          val orderFeedback3 = Factory
            .orderFeedback(orderNewYork, customer, read = Some(true), receivedAt = Some(UtcTime.now.minusDays(3)))
            .create

          Get("/v1/order_feedback.list").addHeader(authorizationHeader) ~> routes ~> check {
            val orderFeedbacks = responseAs[PaginatedApiResponse[Seq[OrderFeedback]]]
            orderFeedbacks.data.map(_.id) ==== Seq(orderFeedback2.id, orderFeedback3.id, orderFeedback1.id)
            assertResponse(orderFeedback1, orderFeedbacks.data.find(_.id == orderFeedback1.id).get)
            assertResponse(orderFeedback2, orderFeedbacks.data.find(_.id == orderFeedback2.id).get)
            assertResponse(orderFeedback3, orderFeedbacks.data.find(_.id == orderFeedback3.id).get)
          }
        }
      }

      "filtered by location_id" should {
        "return a paginated list of all order feedback by location id" in new OrderFeedbackResourceFSpecContext {
          val customer = Factory.globalCustomer(Some(merchant)).create
          val orderRome = Factory.order(merchant, Some(rome)).create
          val orderLondon = Factory.order(merchant, Some(london)).create

          val orderFeedback1 = Factory.orderFeedback(orderRome, customer).create
          val orderFeedback2 = Factory.orderFeedback(orderLondon, customer).create
          val orderFeedback3 = Factory.orderFeedback(orderRome, customer).create

          Get(s"/v1/order_feedback.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val orderFeedbacks = responseAs[PaginatedApiResponse[Seq[OrderFeedback]]]
            orderFeedbacks.data.map(_.id) ==== Seq(orderFeedback1.id, orderFeedback3.id)
            assertResponse(orderFeedback1, orderFeedbacks.data.find(_.id == orderFeedback1.id).get)
            assertResponse(orderFeedback3, orderFeedbacks.data.find(_.id == orderFeedback3.id).get)
          }
        }
      }

      "filtered by customer_id" should {
        "return a paginated list of all order feedback by customer id" in new OrderFeedbackResourceFSpecContext {
          val customer1 = Factory.globalCustomer(Some(merchant)).create
          val customer2 = Factory.globalCustomer(Some(merchant)).create
          val order = Factory.order(merchant, Some(rome)).create

          val orderFeedback1 = Factory.orderFeedback(order, customer1).create
          val orderFeedback2 = Factory.orderFeedback(order, customer2).create
          val orderFeedback3 = Factory.orderFeedback(order, customer2).create

          Get(s"/v1/order_feedback.list?customer_id=${customer1.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            val orderFeedbacks = responseAs[PaginatedApiResponse[Seq[OrderFeedback]]]
            orderFeedbacks.data.map(_.id) ==== Seq(orderFeedback1.id)
            assertResponse(orderFeedback1, orderFeedbacks.data.find(_.id == orderFeedback1.id).get)
          }
        }
      }

      "filtered by status" should {
        "return a paginated list of all order feedback by status" in new OrderFeedbackResourceFSpecContext {
          val customer = Factory.globalCustomer(Some(merchant)).create
          val order = Factory.order(merchant, Some(rome)).create

          val orderFeedback1 = Factory
            .orderFeedback(order, customer, read = Some(true), receivedAt = Some(UtcTime.now.minusDays(1)))
            .create
          val orderFeedback2 = Factory
            .orderFeedback(order, customer, read = Some(false), receivedAt = Some(UtcTime.now.minusDays(2)))
            .create
          val orderFeedback3 = Factory
            .orderFeedback(order, customer, read = Some(true), receivedAt = Some(UtcTime.now.minusDays(3)))
            .create

          Get("/v1/order_feedback.list?status=read").addHeader(authorizationHeader) ~> routes ~> check {
            val orderFeedbacks = responseAs[PaginatedApiResponse[Seq[OrderFeedback]]]
            orderFeedbacks.data.map(_.id) ==== Seq(orderFeedback3.id, orderFeedback1.id)
            assertResponse(orderFeedback1, orderFeedbacks.data.find(_.id == orderFeedback1.id).get)
            assertResponse(orderFeedback3, orderFeedbacks.data.find(_.id == orderFeedback3.id).get)
          }
        }
      }

      "filtered by from date-time" should {
        "return a paginated list of all order feedback and filtered by a start date-time" in new OrderFeedbackResourceFSpecContext {
          val dateTimeInRome = ZonedDateTime.parse("2015-12-03T01:15:30+01:00[Europe/Rome]")
          val dateTimeInHonolulu = ZonedDateTime.parse("2015-12-03T23:59:30-10:00[Pacific/Honolulu]")

          val order = Factory.order(merchant, Some(rome)).create
          val customer = Factory.globalCustomer(Some(merchant)).create

          val orderFeedback1 = Factory.orderFeedback(order, customer, receivedAt = Some(dateTimeInRome)).create
          val orderFeedback2 = Factory.orderFeedback(order, customer, receivedAt = Some(dateTimeInHonolulu)).create
          val orderFeedback3 =
            Factory.orderFeedback(order, customer, receivedAt = Some(dateTimeInRome.minusDays(1))).create

          Get(s"/v1/order_feedback.list?from=2015-12-03T00:00:00").addHeader(authorizationHeader) ~> routes ~> check {
            val orderFeedbacks = responseAs[PaginatedApiResponse[Seq[OrderFeedback]]]
            orderFeedbacks.data.map(_.id) ==== Seq(orderFeedback1.id, orderFeedback2.id)
            assertResponse(orderFeedback1, orderFeedbacks.data.find(_.id == orderFeedback1.id).get)
            assertResponse(orderFeedback2, orderFeedbacks.data.find(_.id == orderFeedback2.id).get)
          }
        }
      }

      "filtered by to date-time" should {
        "return a paginated list of all order feedback and filtered by a start date-time" in new OrderFeedbackResourceFSpecContext {
          val dateTimeInRome = ZonedDateTime.parse("2015-12-03T01:15:30+01:00[Europe/Rome]")
          val dateTimeInHonolulu = ZonedDateTime.parse("2015-12-03T23:59:30-10:00[Pacific/Honolulu]")

          val order = Factory.order(merchant, Some(rome)).create
          val customer = Factory.globalCustomer(Some(merchant)).create

          val orderFeedback1 = Factory.orderFeedback(order, customer, receivedAt = Some(dateTimeInRome)).create
          val orderFeedback2 = Factory.orderFeedback(order, customer, receivedAt = Some(dateTimeInHonolulu)).create
          val orderFeedback3 =
            Factory.orderFeedback(order, customer, receivedAt = Some(dateTimeInRome.minusDays(1))).create

          Get(s"/v1/order_feedback.list?to=2015-12-03T00:00:00").addHeader(authorizationHeader) ~> routes ~> check {
            val orderFeedbacks = responseAs[PaginatedApiResponse[Seq[OrderFeedback]]]
            orderFeedbacks.data.map(_.id) ==== Seq(orderFeedback3.id)
            assertResponse(orderFeedback3, orderFeedbacks.data.find(_.id == orderFeedback3.id).get)
          }
        }
      }

      "with expand[]=customers" should {
        "return a paginated list of all order feedback with customer expanded" in new OrderFeedbackResourceFSpecContext {
          val globalCustomer = Factory.globalCustomer().create
          val customerMerchant = Factory.customerMerchant(merchant, globalCustomer).create

          val order = Factory.order(merchant, Some(rome)).create

          val orderFeedback1 = Factory
            .orderFeedback(order, globalCustomer, read = Some(true), receivedAt = Some(UtcTime.now.minusDays(1)))
            .create
          val orderFeedback2 = Factory
            .orderFeedback(order, globalCustomer, read = Some(false), receivedAt = Some(UtcTime.now.minusDays(2)))
            .create
          val orderFeedback3 = Factory
            .orderFeedback(order, globalCustomer, read = Some(true), receivedAt = Some(UtcTime.now.minusDays(3)))
            .create

          Get("/v1/order_feedback.list?expand[]=customers").addHeader(authorizationHeader) ~> routes ~> check {
            val orderFeedbacks = responseAs[PaginatedApiResponse[Seq[OrderFeedback]]]
            orderFeedbacks.data.map(_.id) ==== Seq(orderFeedback2.id, orderFeedback3.id, orderFeedback1.id)
            assertResponse(
              orderFeedback1,
              orderFeedbacks.data.find(_.id == orderFeedback1.id).get,
              Some(customerMerchant),
            )
            assertResponse(
              orderFeedback2,
              orderFeedbacks.data.find(_.id == orderFeedback2.id).get,
              Some(customerMerchant),
            )
            assertResponse(
              orderFeedback3,
              orderFeedbacks.data.find(_.id == orderFeedback3.id).get,
              Some(customerMerchant),
            )
          }
        }
      }
    }
  }
}
