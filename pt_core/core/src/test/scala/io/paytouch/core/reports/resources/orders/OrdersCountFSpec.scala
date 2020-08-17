package io.paytouch.core.reports.resources.orders

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.enums.{ OrderPaymentType, OrderType, Source }
import io.paytouch.core.reports.entities._

class OrdersCountFSpec extends OrdersFSpec {

  def action = "count"

  class OrdersCountFSpecContext extends OrdersFSpecContext

  "GET /v1/reports/orders.count" in {

    "with no interval" should {

      "without extra params" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$defaultParamsNoInterval").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWhenNoInterval(ReportCount(key = None, count = 3))
          result ==== expectedResult
        }
      }

      "when no items are found" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$emptyParams&group_by=payment_type")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult =
            buildExpectedResultWhenNoInterval(emptyFrom, emptyTo, ReportCount(key = None, count = 0))
          result ==== expectedResult
        }
      }

      "with group by payment type" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$defaultParamsNoInterval&group_by=payment_type")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWhenNoInterval(
            ReportCount(key = OrderPaymentType.Cash, count = 1),
            ReportCount(key = OrderPaymentType.CreditCard, count = 1),
            ReportCount(key = OrderPaymentType.DebitCard, count = 1),
          )
          result ==== expectedResult
        }
      }

      "with group by order type" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$defaultParamsNoInterval&group_by=order_type")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]

          val expectedResult = buildExpectedResultWhenNoInterval(
            ReportCount(key = OrderType.DineIn, count = 2),
            ReportCount(key = OrderType.InStore, count = 1),
          )
          result ==== expectedResult
        }
      }

      "with group by source type" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$defaultParamsNoInterval&group_by=source_type")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]

          val expectedResult = buildExpectedResultWhenNoInterval(
            ReportCount(key = Source.Register, count = 1),
            ReportCount(key = Source.Storefront, count = 2),
          )
          result ==== expectedResult
        }
      }

      "with group by feedback" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$defaultParamsNoInterval&group_by=feedback")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]

          val expectedResult = buildExpectedResultWhenNoInterval(
            ReportCount(key = Some("3"), count = 1),
            ReportCount(key = Some("5"), count = 1),
            ReportCount(key = None, count = 1),
          )
          result ==== expectedResult
        }
      }
    }

    "with interval" should {

      "without extra params" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$defaultParamsWithInterval").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), List(ReportCount(None, 3))),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 0))),
          )
          result ==== expectedResult
        }
      }

      "with group by payment type" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$defaultParamsWithInterval&group_by=payment_type")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(
              ReportTimeframe(start, end),
              List(
                ReportCount(OrderPaymentType.Cash, 1),
                ReportCount(OrderPaymentType.CreditCard, 1),
                ReportCount(OrderPaymentType.DebitCard, 1),
              ),
            ),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 0))),
          )
          result ==== expectedResult
        }
      }

      "with group by order type" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$defaultParamsWithInterval&group_by=order_type")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]

          val expectedResult = buildExpectedResultWithInterval(
            ReportData(
              ReportTimeframe(start, end),
              List(ReportCount(OrderType.DineIn, 2), ReportCount(OrderType.InStore, 1)),
            ),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 0))),
          )
          result ==== expectedResult
        }
      }

      "with group by source type" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$defaultParamsWithInterval&group_by=source_type")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]

          val expectedResult = buildExpectedResultWithInterval(
            ReportData(
              ReportTimeframe(start, end),
              List(ReportCount(Source.Register, 1), ReportCount(Source.Storefront, 2)),
            ),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 0))),
          )
          result ==== expectedResult
        }
      }

      "with group by feedback" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$defaultParamsWithInterval&group_by=feedback")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]

          val expectedResult = buildExpectedResultWithInterval(
            ReportData(
              ReportTimeframe(start, end),
              List(ReportCount(Some("3"), 1), ReportCount(Some("5"), 1), ReportCount(None, 1)),
            ),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 0))),
          )
          result ==== expectedResult
        }
      }
    }

    "filtered by location_id" should {
      "return the correct data" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/orders.count?$defaultParamsWithInterval&location_id=${london.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), List(ReportCount(None, 1))),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 0))),
          )
          result ==== expectedResult
        }
      }
    }
  }
}
