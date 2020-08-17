package io.paytouch.core.reports.resources.customers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities._

class CustomersTopFSpec extends CustomersFSpec {

  def action = "top"

  class CustomersTopFSpecContext extends CustomersFSpecContext {
    val danielaCustomerTop =
      CustomerTop(daniela.id, daniela.firstName, daniela.lastName, -6.59.$$$, 16.$$$, 17.5000, 3)
    val francescoCustomerTop =
      CustomerTop(francesco.id, francesco.firstName, francesco.lastName, -505.54.$$$, 17.$$$, -2932.3529, 2)
    val marcoCustomerTop = CustomerTop(marco.id, marco.firstName, marco.lastName, 791.65.$$$, 1000.$$$, 79.4000, 1)
  }

  "GET /v1/reports/customers.top" in {

    "with no order_by it should reject the request" in new CustomersTopFSpecContext {
      Get(s"/v1/reports/customers.top?$defaultParamsNoInterval")
        .addHeader(authorizationHeader) ~> routes ~> check {
        rejection ==== MissingQueryParamRejection("order_by[]")
      }
    }

    "when no items are found" in new CustomersTopFSpecContext {
      Get(s"/v1/reports/customers.top?$emptyParams&order_by[]=id")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[CustomerTop]]

        val expectedResult = buildExpectedResultWhenNoInterval[CustomerTop](emptyFrom, emptyTo)
        result ==== expectedResult
      }
    }

    "with order by id" in new CustomersTopFSpecContext {
      Get(s"/v1/reports/customers.top?$defaultParamsNoInterval&order_by[]=id")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[CustomerTop]]

        val expectedCustomerTops = Seq(
          danielaCustomerTop,
          francescoCustomerTop,
          marcoCustomerTop,
        )
        val expectedResult = buildExpectedResultWhenNoInterval(expectedCustomerTops.sortBy(_.id.toString): _*)
        result ==== expectedResult
      }
    }

    "with order by first name" in new CustomersTopFSpecContext {
      Get(s"/v1/reports/customers.top?$defaultParamsNoInterval&order_by[]=first_name")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[CustomerTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          danielaCustomerTop,
          francescoCustomerTop,
          marcoCustomerTop,
        )
        result ==== expectedResult
      }
    }

    "with order by last name" in new CustomersTopFSpecContext {
      Get(s"/v1/reports/customers.top?$defaultParamsNoInterval&order_by[]=last_name")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[CustomerTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          marcoCustomerTop,
          francescoCustomerTop,
          danielaCustomerTop,
        )
        result ==== expectedResult
      }
    }

    "with order by visit" in new CustomersTopFSpecContext {
      Get(s"/v1/reports/customers.top?$defaultParamsNoInterval&order_by[]=visit&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[CustomerTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          danielaCustomerTop,
          francescoCustomerTop,
        )
        result ==== expectedResult
      }
    }

    "with order by profit" in new CustomersTopFSpecContext {
      Get(s"/v1/reports/customers.top?$defaultParamsNoInterval&order_by[]=profit&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[CustomerTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          marcoCustomerTop,
          danielaCustomerTop,
        )
        result ==== expectedResult
      }
    }

    "with order by spend" in new CustomersTopFSpecContext {
      Get(s"/v1/reports/customers.top?$defaultParamsNoInterval&order_by[]=spend&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[CustomerTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          marcoCustomerTop,
          francescoCustomerTop,
        )
        result ==== expectedResult
      }
    }

    "with order by margin" in new CustomersTopFSpecContext {
      Get(s"/v1/reports/customers.top?$defaultParamsNoInterval&order_by[]=margin&n=2")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[CustomerTop]]
        val expectedResult = buildExpectedResultWhenNoInterval(
          marcoCustomerTop,
          danielaCustomerTop,
        )
        result ==== expectedResult
      }
    }
  }
}
