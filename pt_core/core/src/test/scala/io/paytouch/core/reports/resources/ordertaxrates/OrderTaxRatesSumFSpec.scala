package io.paytouch.core.reports.resources.ordertaxrates

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.data.model.enums.PaymentStatus
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities.{ ReportFields, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class OrderTaxRatesSumFSpec extends OrderTaxRatesFSpec {

  def action = "sum"

  import fixtures._

  class OrderTaxRatesSumFSpecContext extends OrderTaxRatesFSpecContext

  "GET /v1/reports/order_tax_rates.sum" in {

    "with no interval" should {
      "with no group by" should {
        assertNoField()

        assertFieldResultWhenNoItems(
          "amount",
          ReportFields(key = None, OrderTaxRateAggregate(count = 0, amount = Some(0.$$$))),
        )

        "with field amount" in new OrderTaxRatesSumFSpecContext {
          Get(s"/v1/reports/order_tax_rates.sum?$defaultParamsNoInterval&field[]=amount")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderTaxRateAggregate]]]
            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = None, OrderTaxRateAggregate(count = 5, amount = Some(78.$$$))),
            )
            result ==== expectedResult
          }
        }

        "with field amount and an order with no taxes" in new OrderTaxRatesSumFSpecContext {
          val orderWithNoTaxes = Factory
            .order(
              merchant,
              location = Some(rome),
              receivedAt = Some(now),
              completedAt = Some(now.plusHours(1)),
              isInvoice = Some(false),
              paymentStatus = Some(PaymentStatus.PartiallyRefunded),
            )
            .create
          Get(s"/v1/reports/order_tax_rates.sum?$defaultParamsNoInterval&field[]=amount")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderTaxRateAggregate]]]
            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = None, OrderTaxRateAggregate(count = 5, amount = Some(78.$$$))),
            )
            result ==== expectedResult
          }
        }

        "with all the fields" in new OrderTaxRatesSumFSpecContext {
          Get(s"/v1/reports/order_tax_rates.sum?$defaultParamsNoInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderTaxRateAggregate]]]

            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = None, OrderTaxRateAggregate(count = 5, amount = Some(78.$$$))),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by tax name" should {
        "with all the fields" in new OrderTaxRatesSumFSpecContext {
          Get(s"/v1/reports/order_tax_rates.sum?$defaultParamsNoInterval&field[]=$allFieldsParams&group_by=name")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderTaxRateAggregate]]]

            val expectedResult = buildExpectedResultWhenNoInterval(
              ReportFields(key = Some(taxRate1.name), OrderTaxRateAggregate(count = 2, amount = Some(21.$$$))),
              ReportFields(key = Some(taxRate2.name), OrderTaxRateAggregate(count = 2, amount = Some(42.$$$))),
              ReportFields(key = Some(taxRate3.name), OrderTaxRateAggregate(count = 1, amount = Some(15.$$$))),
            )
            result ==== expectedResult
          }
        }
      }
    }

    "with interval" should {
      "with no group by" should {

        "with no fields it should reject the request" in new OrderTaxRatesSumFSpecContext {
          Get(s"/v1/reports/order_tax_rates.sum?$defaultParamsWithInterval")
            .addHeader(authorizationHeader) ~> routes ~> check {
            rejection ==== MissingQueryParamRejection("field[]")
          }
        }

        "with field amount" in new OrderTaxRatesSumFSpecContext {
          Get(s"/v1/reports/order_tax_rates.sum?$defaultParamsWithInterval&field[]=amount")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderTaxRateAggregate]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(ReportTimeframe(start, end), List(ReportFields(None, OrderTaxRateAggregate(3, Some(30.$$$))))),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(None, OrderTaxRateAggregate(2, Some(48.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
              ),
            )
            result ==== expectedResult
          }
        }

        "with all the fields" in new OrderTaxRatesSumFSpecContext {
          Get(s"/v1/reports/order_tax_rates.sum?$defaultParamsWithInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderTaxRateAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(ReportTimeframe(start, end), List(ReportFields(None, OrderTaxRateAggregate(3, Some(30.$$$))))),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(ReportFields(None, OrderTaxRateAggregate(2, Some(48.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
              ),
            )
            result ==== expectedResult
          }
        }
      }

      "with group by tax name" should {
        "with all the fields" in new OrderTaxRatesSumFSpecContext {
          Get(s"/v1/reports/order_tax_rates.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&group_by=name")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[OrderTaxRateAggregate]]]

            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(Some(taxRate1.name), OrderTaxRateAggregate(1, Some(5.$$$))),
                  ReportFields(Some(taxRate2.name), OrderTaxRateAggregate(1, Some(10.$$$))),
                  ReportFields(Some(taxRate3.name), OrderTaxRateAggregate(1, Some(15.$$$))),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(7), end.plusDays(7)),
                List(
                  ReportFields(Some(taxRate1.name), OrderTaxRateAggregate(1, Some(16.$$$))),
                  ReportFields(Some(taxRate2.name), OrderTaxRateAggregate(1, Some(32.$$$))),
                ),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(14), end.plusDays(14)),
                List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(21), end.plusDays(21)),
                List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
              ),
              ReportData(
                ReportTimeframe(start.plusDays(28), end.plusDays(28)),
                List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
              ),
            )
            result ==== expectedResult
          }
        }
      }
    }

    "filtered by location_id" should {
      "return the correct data" in new OrderTaxRatesSumFSpecContext {
        Get(s"/v1/reports/order_tax_rates.sum?$defaultParamsWithInterval&field[]=amount&location_id=${london.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val result = responseAs[ReportResponse[ReportFields[OrderTaxRateAggregate]]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$))))),
            ReportData(
              ReportTimeframe(start.plusDays(7), end.plusDays(7)),
              List(ReportFields(None, OrderTaxRateAggregate(2, Some(48.$$$)))),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(14), end.plusDays(14)),
              List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(21), end.plusDays(21)),
              List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
            ),
            ReportData(
              ReportTimeframe(start.plusDays(28), end.plusDays(28)),
              List(ReportFields(None, OrderTaxRateAggregate(0, Some(0.$$$)))),
            ),
          )
          result ==== expectedResult
        }
      }
    }
  }
}
