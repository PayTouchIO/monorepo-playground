package io.paytouch.core.reports.resources.sales

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection

import io.paytouch.core.data.model.enums.TransactionPaymentType._
import io.paytouch.core.entities.MonetaryAmount
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities._

class SalesAverageFSpec extends SalesFSpec {

  def action = "average"

  import fixtures._

  val fullAggregate =
    SalesAggregate(
      count = totalCount,
      costs = Some(3.30.$$$),
      discounts = Some(3.53.$$$),
      giftCardSales = Some(0.$$$),
      grossProfits = Some(-2.8.$$$),
      grossSales = Some(4.$$$),
      netSales = Some(0.5.$$$),
      nonTaxable = Some(0.25.$$$),
      refunds = Some(10.75.$$$),
      taxable = Some(2.5.$$$),
      taxes = Some(1.25.$$$),
      tips = Some(2.25.$$$),
      tenderTypes = Some(
        Map(
          CreditCard -> MonetaryAmount(-19.50, currency),
          DebitCard -> MonetaryAmount(5.00, currency),
          GiftCard -> MonetaryAmount(0.50, currency),
          Cash -> MonetaryAmount(0.33, currency),
        ),
      ),
    )

  val zeroResult = SalesAggregate.zero(true)

  "GET /v1/reports/sales.average" in {

    "with no interval" should {
      "with no group by" should {
        val emptyAggregate = SalesAggregate(count = totalCount)

        assertNoField()

        assertFieldResult(
          "gross_sales",
          ReportFields(key = None, emptyAggregate.copy(grossSales = fullAggregate.grossSales)),
        )

        assertFieldResult("net_sales", ReportFields(key = None, emptyAggregate.copy(netSales = fullAggregate.netSales)))

        assertFieldResult(
          "discounts",
          ReportFields(key = None, emptyAggregate.copy(discounts = fullAggregate.discounts)),
        )

        assertFieldResult("refunds", ReportFields(key = None, emptyAggregate.copy(refunds = fullAggregate.refunds)))

        assertFieldResult("taxes", ReportFields(key = None, emptyAggregate.copy(taxes = fullAggregate.taxes)))

        assertFieldResult("tips", ReportFields(key = None, emptyAggregate.copy(tips = fullAggregate.tips)))

        assertFieldResult(
          "tender_types",
          ReportFields(key = None, emptyAggregate.copy(tenderTypes = fullAggregate.tenderTypes)),
        )

        assertAllFieldsResult(ReportFields(key = None, fullAggregate))
      }
    }

    "with interval" should {
      "with no group by" should {
        "with no fields it should reject the request" in new SalesFSpecContext {
          Get(s"/v1/reports/sales.average?$defaultParamsWithInterval")
            .addHeader(authorizationHeader) ~> routes ~> check {
            rejection ==== MissingQueryParamRejection("field[]")
          }
        }

        "with field profit" in new SalesFSpecContext {
          Get(s"/v1/reports/sales.average?$defaultParamsWithInterval&field[]=gross_sales")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val zeroResult = SalesAggregate(
              count = 0,
              grossSales = Some(0.$$$),
            )
            val result = responseAs[ReportResponse[ReportFields[SalesAggregate]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                List(
                  ReportFields(
                    None,
                    SalesAggregate(
                      count = totalCount,
                      grossSales = fullAggregate.grossSales,
                    ),
                  ),
                ),
              ),
              ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportFields(None, zeroResult))),
              ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportFields(None, zeroResult))),
              ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportFields(None, zeroResult))),
              ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportFields(None, zeroResult))),
            )
            result ==== expectedResult
          }
        }

        "with all fields" in new SalesFSpecContext {
          Get(s"/v1/reports/sales.average?$defaultParamsWithInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val result = responseAs[ReportResponse[ReportFields[SalesAggregate]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(ReportTimeframe(start, end), List(ReportFields(None, fullAggregate))),
              ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportFields(None, zeroResult))),
              ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportFields(None, zeroResult))),
              ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportFields(None, zeroResult))),
              ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportFields(None, zeroResult))),
            )
            result ==== expectedResult
          }
        }
      }
    }

    "filtered by location_id" in new SalesFSpecContext {
      Get(s"/v1/reports/sales.average?$defaultParamsWithInterval&field[]=gross_sales&location_id=${london.id}")
        .addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val zeroResult = SalesAggregate(
          count = 0,
          grossSales = Some(0.$$$),
        )
        val result = responseAs[ReportResponse[ReportFields[SalesAggregate]]]
        val expectedResult = buildExpectedResultWithInterval(
          ReportData(
            ReportTimeframe(start, end),
            List(
              ReportFields(
                None,
                SalesAggregate(
                  count = 1,
                  grossSales = Some(5.$$$),
                ),
              ),
            ),
          ),
          ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportFields(None, zeroResult))),
          ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportFields(None, zeroResult))),
          ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), List(ReportFields(None, zeroResult))),
          ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), List(ReportFields(None, zeroResult))),
        )
        result ==== expectedResult
      }
    }
  }
}
