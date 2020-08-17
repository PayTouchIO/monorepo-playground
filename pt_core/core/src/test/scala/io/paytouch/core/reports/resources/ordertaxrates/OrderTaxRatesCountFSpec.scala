package io.paytouch.core.reports.resources.ordertaxrates

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.reports.entities._

class OrderTaxRatesCountFSpec extends OrderTaxRatesFSpec {

  def action = "count"

  class OrdersCountFSpecContext extends OrderTaxRatesFSpecContext

  "GET /v1/reports/order_tax_rates.count" in {

    "with no interval" should {

      "without extra params" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/order_tax_rates.count?$defaultParamsNoInterval")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWhenNoInterval(ReportCount(key = None, count = 5))
          result ==== expectedResult
        }
      }

      "when no items are found" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/order_tax_rates.count?$emptyParams&group_by=name")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult =
            buildExpectedResultWhenNoInterval(emptyFrom, emptyTo, ReportCount(key = None, count = 0))
          result ==== expectedResult
        }
      }

      "with group by name" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/order_tax_rates.count?$defaultParamsNoInterval&group_by=name")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWhenNoInterval(
            ReportCount(key = Some(taxRate1.name), count = 2),
            ReportCount(key = Some(taxRate2.name), count = 2),
            ReportCount(key = Some(taxRate3.name), count = 1),
          )
          result ==== expectedResult
        }
      }
    }

    "with interval" should {

      "without extra params" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/order_tax_rates.count?$defaultParamsWithInterval")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), List(ReportCount(None, 3))),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(None, 2))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 0))),
          )
          result ==== expectedResult
        }
      }

      "with group by name" in new OrdersCountFSpecContext {
        Get(s"/v1/reports/order_tax_rates.count?$defaultParamsWithInterval&group_by=name")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(
              ReportTimeframe(start, end),
              List(
                ReportCount(Some(taxRate1.name), 1),
                ReportCount(Some(taxRate2.name), 1),
                ReportCount(Some(taxRate3.name), 1),
              ),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(7), end.plusDays(7)),
              List(ReportCount(Some(taxRate1.name), 1), ReportCount(Some(taxRate2.name), 1)),
            ),
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
        Get(s"/v1/reports/order_tax_rates.count?$defaultParamsWithInterval&location_id=${london.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(None, 2))),
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
