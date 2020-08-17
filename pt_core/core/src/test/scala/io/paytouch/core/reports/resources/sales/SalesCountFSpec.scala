package io.paytouch.core.reports.resources.sales

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.reports.entities._

class SalesCountFSpec extends SalesFSpec {

  def action = "count"

  class SalesCountFSpecContext extends SalesFSpecContext

  "GET /v1/reports/sales.count" in {

    "with no interval" should {

      "without extra params" in new SalesCountFSpecContext {
        Get(s"/v1/reports/sales.count?$defaultParamsNoInterval").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWhenNoInterval(ReportCount(key = None, count = totalCount))
          result ==== expectedResult
        }
      }

      "when no items are found" in new SalesCountFSpecContext {
        Get(s"/v1/reports/sales.count?$emptyParams")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult =
            buildExpectedResultWhenNoInterval(emptyFrom, emptyTo, ReportCount(key = None, count = 0))
          result ==== expectedResult
        }
      }
    }

    "with interval" should {

      "without extra params" in new SalesCountFSpecContext {
        Get(s"/v1/reports/sales.count?$defaultParamsWithInterval").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), List(ReportCount(None, totalCount))),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 0))),
          )
          result ==== expectedResult
        }
      }
    }

    "filtered by location_id" in new SalesCountFSpecContext {
      Get(s"/v1/reports/sales.count?$defaultParamsWithInterval&location_id=${london.id}")
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
