package io.paytouch.core.reports.resources.customers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums.{ CustomerType, LoyaltyProgramStatus }

class CustomersSumFSpec extends CustomersFSpec {

  def action = "sum"

  import fixtures._

  class CustomersSumFSpecContext extends CustomersFSpecContext

  "GET /v1/reports/customers.sum" in {

    "with no interval" should {
      "with no group by" should {
        assertNoField()

        assertFieldResultWhenNoItems(
          "spend",
          ReportFields(key = None, CustomerAggregate(count = 0, spend = Some(0.$$$))),
        )

        "with field spend" in new CustomersSumFSpecContext {
          Get(s"/v1/reports/customers.sum?$defaultParamsNoInterval&field[]=spend")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[CustomerAggregate]]]
            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = None, CustomerAggregate(count = 3, spend = Some(1033.$$$))),
            )
            result ==== expectedResult
          }
        }

        "with all the fields" in new CustomersSumFSpecContext {
          Get(s"/v1/reports/customers.sum?$defaultParamsNoInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[CustomerAggregate]]]

            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = None, CustomerAggregate(count = 3, spend = Some(1033.$$$))),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by customer type" should {
        "with all the fields" in new CustomersSumFSpecContext {
          Get(s"/v1/reports/customers.sum?$defaultParamsNoInterval&field[]=$allFieldsParams&group_by=type")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[CustomerAggregate]]]

            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = CustomerType.New, CustomerAggregate(count = 2, spend = Some(1017.$$$))),
              ReportFields(key = CustomerType.Returning, CustomerAggregate(count = 1, spend = Some(16.$$$))),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by visit" should {
        "with all the fields" in new CustomersSumFSpecContext {
          Get(s"/v1/reports/customers.sum?$defaultParamsNoInterval&field[]=$allFieldsParams&group_by=visit")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[CustomerAggregate]]]

            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = Some("1"), CustomerAggregate(count = 1, spend = Some(1000.$$$))),
              ReportFields(key = Some("2"), CustomerAggregate(count = 1, spend = Some(17.$$$))),
              ReportFields(key = Some("3"), CustomerAggregate(count = 1, spend = Some(16.$$$))),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by loyalty program" should {
        "with all the fields" in new CustomersSumFSpecContext {
          Get(s"/v1/reports/customers.sum?$defaultParamsNoInterval&field[]=$allFieldsParams&group_by=loyalty_program")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[CustomerAggregate]]]

            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = LoyaltyProgramStatus.With, CustomerAggregate(count = 1, spend = Some(16.$$$))),
              ReportFields(key = LoyaltyProgramStatus.Without, CustomerAggregate(count = 2, spend = Some(1017.$$$))),
            )
            result ==== expectedResult
          }
        }
      }

    }

    "with interval" should {
      "with no group by" should {

        "with no fields it should reject the request" in new CustomersSumFSpecContext {
          Get(s"/v1/reports/customers.sum?$defaultParamsWithInterval")
            .addHeader(authorizationHeader) ~> routes ~> check {
            rejection ==== MissingQueryParamRejection("field[]")
          }
        }

        "with field spend" in new CustomersSumFSpecContext {
          Get(s"/v1/reports/customers.sum?$defaultParamsWithInterval&field[]=spend")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[CustomerAggregate]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(ReportTimeframe(start, end), List(ReportFields(None, CustomerAggregate(3, Some(1021.$$$))))),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(None, CustomerAggregate(1, Some(5.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(None, CustomerAggregate(1, Some(1.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(None, CustomerAggregate(0, Some(0.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(None, CustomerAggregate(1, Some(6.$$$)))),
              ),
            )
            result ==== expectedResult
          }
        }

        "with all the fields" in new CustomersSumFSpecContext {
          Get(s"/v1/reports/customers.sum?$defaultParamsWithInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[CustomerAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(ReportTimeframe(start, end), List(ReportFields(None, CustomerAggregate(3, Some(1021.$$$))))),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(None, CustomerAggregate(1, Some(5.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(None, CustomerAggregate(1, Some(1.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(None, CustomerAggregate(0, Some(0.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(None, CustomerAggregate(1, Some(6.$$$)))),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by customer type" should {
        "with all the fields" in new CustomersSumFSpecContext {
          Get(s"/v1/reports/customers.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&group_by=type")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[CustomerAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(CustomerType.New, CustomerAggregate(2, Some(1011.$$$))),
                  ReportFields(CustomerType.Returning, CustomerAggregate(1, Some(10.$$$))),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(CustomerType.Returning, CustomerAggregate(1, Some(5.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(CustomerType.Returning, CustomerAggregate(1, Some(1.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(CustomerType.New, CustomerAggregate(0, Some(0.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(CustomerType.Returning, CustomerAggregate(1, Some(6.$$$)))),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by visit" should {
        "with all the fields" in new CustomersSumFSpecContext {
          Get(s"/v1/reports/customers.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&group_by=visit")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[CustomerAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(ReportFields(Some("1"), CustomerAggregate(3, Some(1021.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(Some("1"), CustomerAggregate(1, Some(5.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(Some("1"), CustomerAggregate(1, Some(1.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(Some("0"), CustomerAggregate(0, Some(0.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(Some("1"), CustomerAggregate(1, Some(6.$$$)))),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by loyalty program" should {
        "with all the fields" in new CustomersSumFSpecContext {
          Get(s"/v1/reports/customers.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&group_by=loyalty_program")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[CustomerAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(LoyaltyProgramStatus.With, CustomerAggregate(1, Some(10.$$$))),
                  ReportFields(LoyaltyProgramStatus.Without, CustomerAggregate(2, Some(1011.$$$))),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(LoyaltyProgramStatus.With, CustomerAggregate(1, Some(5.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(LoyaltyProgramStatus.With, CustomerAggregate(1, Some(1.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(LoyaltyProgramStatus.Without, CustomerAggregate(0, Some(0.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(LoyaltyProgramStatus.With, CustomerAggregate(1, Some(6.$$$)))),
              ),
            )
            result ==== expectedResult
          }
        }
      }
    }

    "with location_id" should {
      "return data filtered by location id" in new CustomersSumFSpecContext {
        Get(s"/v1/reports/customers.sum?$defaultParamsNoInterval&field[]=spend&location_id=${london.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportFields[CustomerAggregate]]]
          val expectedResult = buildExpectedResultWhenNoInterval(
            ReportFields(key = None, CustomerAggregate(count = 1, spend = Some(1000.$$$))),
          )
          result ==== expectedResult
        }
      }
    }
  }
}
