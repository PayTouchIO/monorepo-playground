package io.paytouch.core.reports.resources.customers

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums.{ CustomerType, LoyaltyProgramStatus }

class CustomersCountFSpec extends CustomersFSpec {

  def action = "count"

  class CustomersCountFSpecContext extends CustomersFSpecContext

  "GET /v1/reports/customers.count" in {

    "with no interval" should {

      "without extra params" in new CustomersCountFSpecContext {
        Get(s"/v1/reports/customers.count?$defaultParamsNoInterval")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWhenNoInterval(ReportCount(key = None, count = 3))
          result ==== expectedResult
        }
      }

      "when no items are found" in new CustomersCountFSpecContext {
        Get(s"/v1/reports/customers.count?$emptyParams")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult =
            buildExpectedResultWhenNoInterval(emptyFrom, emptyTo, ReportCount(key = None, count = 0))
          result ==== expectedResult
        }
      }

      "with group by customer type" in new CustomersCountFSpecContext {
        Get(s"/v1/reports/customers.count?$defaultParamsNoInterval&group_by=type")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]

          val expectedResult = buildExpectedResultWhenNoInterval(
            ReportCount(CustomerType.New, 2),
            ReportCount(CustomerType.Returning, 1),
          )
          result ==== expectedResult
        }
      }

      "with group by visit" in new CustomersCountFSpecContext {
        Get(s"/v1/reports/customers.count?$defaultParamsNoInterval&group_by=visit")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]

          val expectedResult = buildExpectedResultWhenNoInterval(
            ReportCount(Some("1"), 1),
            ReportCount(Some("2"), 1),
            ReportCount(Some("3"), 1),
          )
          result ==== expectedResult
        }
      }

      "with group by loyalty program" in new CustomersCountFSpecContext {
        Get(s"/v1/reports/customers.count?$defaultParamsNoInterval&group_by=loyalty_program")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]

          val expectedResult = buildExpectedResultWhenNoInterval(
            ReportCount(LoyaltyProgramStatus.With, 1),
            ReportCount(LoyaltyProgramStatus.Without, 2),
          )
          result ==== expectedResult
        }
      }
    }

    "with interval" should {

      "without extra params" in new CustomersCountFSpecContext {
        Get(s"/v1/reports/customers.count?$defaultParamsWithInterval")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), List(ReportCount(None, 3))),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(None, 1))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(None, 1))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(None, 0))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(None, 1))),
          )
          result ==== expectedResult
        }
      }

      "with group by customer type" in new CustomersCountFSpecContext {
        Get(s"/v1/reports/customers.count?$defaultParamsWithInterval&group_by=type")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(
              ReportTimeframe(start, end),
              List(ReportCount(CustomerType.New, 2), ReportCount(CustomerType.Returning, 1)),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(7), end.plusDays(7)),
              List(ReportCount(CustomerType.Returning, 1)),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(14), end.plusDays(14)),
              List(ReportCount(CustomerType.Returning, 1)),
            ),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(CustomerType.New, 0))),
            ReportData(
              ReportTimeframe(start.plusDays(28), end.plusDays(28)),
              List(ReportCount(CustomerType.Returning, 1)),
            ),
          )
          result ==== expectedResult
        }
      }

      "with group by visit" in new CustomersCountFSpecContext {
        Get(s"/v1/reports/customers.count?$defaultParamsWithInterval&group_by=visit")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), List(ReportCount(Some("1"), 3))),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportCount(Some("1"), 1))),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportCount(Some("1"), 1))),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportCount(Some("0"), 0))),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportCount(Some("1"), 1))),
          )
          result ==== expectedResult
        }
      }

      "with group by loyalty program" in new CustomersCountFSpecContext {
        Get(s"/v1/reports/customers.count?$defaultParamsWithInterval&group_by=loyalty_program")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(
              ReportTimeframe(start, end),
              List(ReportCount(LoyaltyProgramStatus.With, 1), ReportCount(LoyaltyProgramStatus.Without, 2)),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(7), end.plusDays(7)),
              List(ReportCount(LoyaltyProgramStatus.With, 1)),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(14), end.plusDays(14)),
              List(ReportCount(LoyaltyProgramStatus.With, 1)),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(21), end.plusDays(21)),
              List(ReportCount(LoyaltyProgramStatus.Without, 0)),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(28), end.plusDays(28)),
              List(ReportCount(LoyaltyProgramStatus.With, 1)),
            ),
          )
          result ==== expectedResult
        }
      }
    }

    "with location_id" should {
      "return data filtered by location id" in new CustomersCountFSpecContext {
        Get(s"/v1/reports/customers.count?$defaultParamsNoInterval&location_id=${london.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportCount]]
          val expectedResult = buildExpectedResultWhenNoInterval(ReportCount(key = None, count = 1))
          result ==== expectedResult
        }
      }
    }
  }
}
