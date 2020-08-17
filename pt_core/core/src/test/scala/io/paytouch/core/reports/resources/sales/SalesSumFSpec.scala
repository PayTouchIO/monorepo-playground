package io.paytouch.core.reports.resources.sales

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.data.model.enums.TransactionPaymentType._
import io.paytouch.core.entities.MonetaryAmount
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities.{ ReportFields, ReportTimeframe, _ }

class SalesSumFSpec extends SalesFSpec {

  def action = "sum"

  import fixtures._

  val fullAggregate =
    SalesAggregate(
      count = totalCount,
      costs = Some(13.20.$$$),
      discounts = Some(14.10.$$$),
      giftCardSales = Some(0.$$$),
      grossProfits = Some(-11.20.$$$),
      grossSales = Some(16.00.$$$),
      netSales = Some(2.$$$),
      nonTaxable = Some(1.00.$$$),
      refunds = Some(43.00.$$$),
      taxable = Some(10.00.$$$),
      taxes = Some(5.00.$$$),
      tips = Some(9.00.$$$),
      tenderTypes = Some(
        Map(
          CreditCard -> MonetaryAmount(-39.00, currency),
          DebitCard -> MonetaryAmount(5.00, currency),
          GiftCard -> MonetaryAmount(1.00, currency),
          Cash -> MonetaryAmount(1.00, currency),
        ),
      ),
    )

  val zeroResult = SalesAggregate.zero(withTenderTypes = true)

  "GET /v1/reports/sales.sum" in {

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

        s"with order_types[] filter" in {
          val filteredFullAggregate = SalesAggregate(
            count = 2,
            costs = Some(12.50.$$$),
            discounts = Some(11.80.$$$),
            giftCardSales = Some(0.$$$),
            grossProfits = Some(-10.50.$$$),
            grossSales = Some(15.00.$$$),
            netSales = Some(2.$$$),
            nonTaxable = Some(0.$$$),
            refunds = Some(41.00.$$$),
            taxable = Some(10.00.$$$),
            taxes = Some(5.00.$$$),
            tips = Some(8.00.$$$),
            tenderTypes = Some(
              Map(
                CreditCard -> MonetaryAmount(-39.00, currency),
                DebitCard -> MonetaryAmount(5.00, currency),
                GiftCard -> MonetaryAmount(1.00, currency),
                Cash -> MonetaryAmount(0.00, currency),
              ),
            ),
          )
          assertResponseWithDefaultDates(
            s"/v1/reports/sales.sum?${fixtures.defaultParamsNoInterval}&field[]=${fixtures.allFieldsParams}&order_types[]=dine_in",
            None,
            ReportFields(key = None, filteredFullAggregate),
          )
        }
      }
    }

    "with interval" should {

      "with no group by" should {

        "with no fields it should reject the request" in new SalesFSpecContext {
          Get(s"/v1/reports/sales.sum?$defaultParamsWithInterval").addHeader(authorizationHeader) ~> routes ~> check {
            rejection ==== MissingQueryParamRejection("field[]")
          }
        }

        "with field profit" in new SalesFSpecContext {
          Get(s"/v1/reports/sales.sum?$defaultParamsWithInterval&field[]=gross_sales")
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
          Get(s"/v1/reports/sales.sum?$defaultParamsWithInterval&field[]=$allFieldsParams")
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

    "with force interval" should {

      "with no group by" should {

        "with all fields" in new SalesFSpecContext {
          Get(
            s"/v1/reports/sales.sum?from=$from&to=${from.plusDays(14)}&force_interval=weekly&field[]=$allFieldsParams",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val result = responseAs[ReportResponse[ReportFields[SalesAggregate]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(ReportTimeframe(start, end), List(ReportFields(None, fullAggregate))),
              ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), List(ReportFields(None, zeroResult))),
              ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), List(ReportFields(None, zeroResult))),
            )
            result ==== expectedResult
          }
        }
      }
    }

    "filtered by location_id" in new SalesFSpecContext {
      Get(s"/v1/reports/sales.sum?$defaultParamsWithInterval&field[]=gross_sales&location_id=${london.id}")
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
