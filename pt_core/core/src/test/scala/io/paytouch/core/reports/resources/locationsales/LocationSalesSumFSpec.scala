package io.paytouch.core.reports.resources.locationsales

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities.{ ReportFields, ReportTimeframe, _ }

class LocationSalesSumFSpec extends LocationSalesFSpec {

  def action = "sum"

  import fixtures._

  "GET /v1/reports/location_sales.sum" in {

    "with no interval" should {
      "with no group by" should {
        assertNoField()

        assertFieldResult(
          "gross_sales",
          orderedResult(
            londonData = londonEmptyAggregate.copy(grossSales = londonFullAggregate.grossSales),
            romeData = romeEmptyAggregate.copy(grossSales = romeFullAggregate.grossSales),
          ): _*,
        )

        assertFieldResult(
          "net_sales",
          orderedResult(
            londonData = londonEmptyAggregate.copy(netSales = londonFullAggregate.netSales),
            romeData = romeEmptyAggregate.copy(netSales = romeFullAggregate.netSales),
          ): _*,
        )

        assertFieldResult(
          "discounts",
          orderedResult(
            londonData = londonEmptyAggregate.copy(discounts = londonFullAggregate.discounts),
            romeData = romeEmptyAggregate.copy(discounts = romeFullAggregate.discounts),
          ): _*,
        )

        assertFieldResult(
          "refunds",
          orderedResult(
            londonData = londonEmptyAggregate.copy(refunds = londonFullAggregate.refunds),
            romeData = romeEmptyAggregate.copy(refunds = romeFullAggregate.refunds),
          ): _*,
        )

        assertFieldResult(
          "taxes",
          orderedResult(
            londonData = londonEmptyAggregate.copy(taxes = londonFullAggregate.taxes),
            romeData = romeEmptyAggregate.copy(taxes = romeFullAggregate.taxes),
          ): _*,
        )

        assertFieldResult(
          "tips",
          orderedResult(
            londonData = londonEmptyAggregate.copy(tips = londonFullAggregate.tips),
            romeData = romeEmptyAggregate.copy(tips = romeFullAggregate.tips),
          ): _*,
        )

        assertFieldResult(
          "tender_types",
          orderedResult(
            londonData = londonEmptyAggregate.copy(tenderTypes = londonFullAggregate.tenderTypes),
            romeData = romeEmptyAggregate.copy(tenderTypes = romeFullAggregate.tenderTypes),
          ): _*,
        )

        assertAllFieldsResult(
          orderedResult(
            londonData = londonFullAggregate,
            romeData = romeFullAggregate,
          ): _*,
        )
      }
    }

    "with interval" should {

      "with no group by" should {

        "with no fields it should reject the request" in {
          Get(s"/v1/reports/location_sales.sum?$defaultParamsWithInterval")
            .addHeader(authorizationHeader) ~> routes ~> check {
            rejection ==== MissingQueryParamRejection("field[]")
          }
        }

        "with field gross_sales" in new SalesFSpecContext {
          Get(s"/v1/reports/location_sales.sum?$defaultParamsWithInterval&field[]=gross_sales")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val zeroResult = SalesAggregate(
              count = 0,
              grossSales = Some(0.$$$),
            )
            val zeroOrderedResult = orderedResult(
              londonData = zeroResult,
              romeData = zeroResult,
            )

            val result = responseAs[ReportResponse[ReportFields[LocationSales]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                orderedResult(
                  londonData = londonEmptyAggregate.copy(grossSales = londonFullAggregate.grossSales),
                  romeData = romeEmptyAggregate.copy(grossSales = romeFullAggregate.grossSales),
                ),
              ),
              ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), zeroOrderedResult),
              ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), zeroOrderedResult),
              ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), zeroOrderedResult),
              ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), zeroOrderedResult),
            )
            result ==== expectedResult
          }
        }

        "with all fields" in new SalesFSpecContext {
          Get(s"/v1/reports/location_sales.sum?$defaultParamsWithInterval&field[]=$allFieldsParams")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val zeroResult = SalesAggregate.zero(withTenderTypes = true)
            val zeroOrderedResult = orderedResult(
              londonData = zeroResult,
              romeData = zeroResult,
            )

            val result = responseAs[ReportResponse[ReportFields[LocationSales]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                orderedResult(
                  londonData = londonFullAggregate,
                  romeData = romeFullAggregate,
                ),
              ),
              ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), zeroOrderedResult),
              ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), zeroOrderedResult),
              ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), zeroOrderedResult),
              ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), zeroOrderedResult),
            )
            result ==== expectedResult
          }
        }
      }
    }

    "filtered by location" should {
      "with all fields" in new SalesFSpecContext {
        Get(
          s"/v1/reports/location_sales.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&location_id=${london.id}",
        ).addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()
          val zeroResult = SalesAggregate.zero(withTenderTypes = true)
          val zeroOrderedResult = List(londonResult(zeroResult))

          val result = responseAs[ReportResponse[ReportFields[LocationSales]]]
          val expectedResult = buildExpectedResultWithInterval(
            ReportData(ReportTimeframe(start, end), Seq(londonResult(londonFullAggregate))),
            ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), zeroOrderedResult),
            ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), zeroOrderedResult),
            ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), zeroOrderedResult),
            ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), zeroOrderedResult),
          )
          result ==== expectedResult
        }
      }
    }
  }
}
