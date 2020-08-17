package io.paytouch.core.reports.resources.productsales

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities._
import org.specs2.specification.core.Fragment

class ProductSalesSumFSpec extends ProductSalesAggrFSpec {

  def action = "sum"

  import fixtures._

  "GET /v1/reports/product_sales.sum" in {

    "with no interval" should {
      "with no group by" should {

        "with no fields it should reject the request" in {
          Get(s"/v1/reports/${view.endpoint}.$action?$defaultParamsNoInterval&id[]=${ids.mkString(",")}")
            .addHeader(fixtures.authorizationHeader) ~> fixtures.routes ~> check {
            rejection ==== MissingQueryParamRejection("field[]")
          }
        }

        "with no ids it should reject the request" in {
          Get(s"/v1/reports/${view.endpoint}.$action?$defaultParamsNoInterval&field[]=discounts")
            .addHeader(fixtures.authorizationHeader) ~> fixtures.routes ~> check {
            rejection ==== MissingQueryParamRejection("id[]")
          }
        }

        def assertFieldResultAndDefaults(
            field: String,
            f: (OrderItemSalesAggregate, OrderItemSalesAggregate) => OrderItemSalesAggregate,
          ): Fragment = {
          val expectedResults = combineEmptyWithFull(f)
          s"with field $field" in {
            assertResponseWithDefaultDates(
              s"/v1/reports/${view.endpoint}.$action?$defaultParamsNoInterval&field[]=$field&id[]=${ids
                .mkString(",")}",
              None,
              expectedResults: _*,
            )
          }
        }

        def assertAllFieldsResult[T](expectedResults: T*)(implicit um: FromResponseUnmarshaller[ReportResponse[T]]) =
          s"with all fields" in {
            assertResponseWithDefaultDates(
              s"/v1/reports/${view.endpoint}.$action?$defaultParamsNoInterval&field[]=${fixtures.allFieldsParams}&id[]=${ids
                .mkString(",")}",
              None,
              expectedResults: _*,
            )
          }

        assertFieldResultAndDefaults("discounts", (empty, full) => empty.copy(discounts = full.discounts))

        assertFieldResultAndDefaults("gross_profits", (empty, full) => empty.copy(grossProfits = full.grossProfits))

        assertFieldResultAndDefaults("gross_sales", (empty, full) => empty.copy(grossSales = full.grossSales))

        assertFieldResultAndDefaults("net_sales", (empty, full) => empty.copy(netSales = full.netSales))

        assertFieldResultAndDefaults("margin", (empty, full) => empty.copy(margin = full.margin))

        assertFieldResultAndDefaults("quantity", (empty, full) => empty.copy(quantity = full.quantity))

        assertFieldResultAndDefaults(
          "returned_quantity",
          (empty, full) => empty.copy(returnedQuantity = full.returnedQuantity),
        )

        assertFieldResultAndDefaults(
          "returned_amount",
          (empty, full) => empty.copy(returnedAmount = full.returnedAmount),
        )

        assertFieldResultAndDefaults("cost", (empty, full) => empty.copy(cost = full.cost))

        assertFieldResultAndDefaults("taxable", (empty, full) => empty.copy(taxable = full.taxable))

        assertFieldResultAndDefaults("non_taxable", (empty, full) => empty.copy(nonTaxable = full.nonTaxable))

        assertFieldResultAndDefaults("taxes", (empty, full) => empty.copy(taxes = full.taxes))

        assertAllFieldsResult(resultFieldsOrdered123: _*)
      }
    }

    "with interval" should {

      "with no group by" should {

        "with no fields it should reject the request" in {
          Get(s"/v1/reports/${view.endpoint}.$action?$defaultParamsWithInterval&id[]=${ids.mkString(",")}")
            .addHeader(fixtures.authorizationHeader) ~> fixtures.routes ~> check {
            rejection ==== MissingQueryParamRejection("field[]")
          }
        }

        "with no ids it should reject the request" in {
          Get(s"/v1/reports/${view.endpoint}.$action?$defaultParamsWithInterval&field[]=discounts")
            .addHeader(fixtures.authorizationHeader) ~> fixtures.routes ~> check {
            rejection ==== MissingQueryParamRejection("id[]")
          }
        }

        "with field discounts" in new ProductSalesAggrFSpecContext {
          Get(s"/v1/reports/product_sales.sum?$defaultParamsWithInterval&field[]=discounts&id[]=${ids.mkString(",")}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val zeroResult = OrderItemSalesAggregate(
              count = 0,
              discounts = Some(0.$$$),
            )
            val zeroOrderedResult = emptyResultFieldsOrdered123.map { fields =>
              val newValues = fields.values.copy(data = zeroResult)
              fields.copy(values = newValues)
            }

            val orderedResult = combineEmptyWithFull((empty, full) => empty.copy(discounts = full.discounts))

            val result = responseAs[ReportResponse[ReportFields[ProductSales]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                orderedResult,
              ),
              ReportData(ReportTimeframe(start.plusDays(7), end.plusDays(7)), zeroOrderedResult),
              ReportData(ReportTimeframe(start.plusDays(14), end.plusDays(14)), zeroOrderedResult),
              ReportData(ReportTimeframe(start.plusDays(21), end.plusDays(21)), zeroOrderedResult),
              ReportData(ReportTimeframe(start.plusDays(28), end.plusDays(28)), zeroOrderedResult),
            )
            result ==== expectedResult
          }
        }

        "with all fields" in new ProductSalesAggrFSpecContext {
          Get(
            s"/v1/reports/product_sales.sum?$defaultParamsWithInterval&field[]=$allFieldsParams&id[]=${ids.mkString(",")}",
          ).addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val zeroResult = OrderItemSalesAggregate(
              count = 0,
              discounts = Some(0.$$$),
              grossProfits = Some(0.$$$),
              grossSales = Some(0.$$$),
              margin = Some(0),
              netSales = Some(0.$$$),
              quantity = Some(0),
              returnedQuantity = Some(0),
              returnedAmount = Some(0.$$$),
              cost = Some(0.00.$$$),
              taxable = Some(0.00.$$$),
              nonTaxable = Some(0.00.$$$),
              taxes = Some(0.$$$),
            )
            val zeroOrderedResult = emptyResultFieldsOrdered123.map { fields =>
              val newValues = fields.values.copy(data = zeroResult)
              fields.copy(values = newValues)
            }

            val result = responseAs[ReportResponse[ReportFields[ProductSales]]]
            val expectedResult = buildExpectedResultWithInterval(
              ReportData(
                ReportTimeframe(start, end),
                resultFieldsOrdered123,
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

    "filtered by location_id" in new ProductSalesAggrFSpecContext {
      Get(
        s"/v1/reports/product_sales.sum?$defaultParamsWithInterval&field[]=quantity&id[]=${simpleProduct1.id}&location_id=${london.id}",
      ).addHeader(authorizationHeader) ~> routes ~> check {
        assertStatusOK()

        val zeroOrderedResult = Seq(
          ReportFields(
            key = Some(simpleProduct1.id.toString),
            values = ProductSales(simpleProduct1, OrderItemSalesAggregate(count = 0, quantity = Some(0))),
          ),
        )

        val result = responseAs[ReportResponse[ReportFields[ProductSales]]]
        val expectedResult = buildExpectedResultWithInterval(
          ReportData(
            ReportTimeframe(start, end),
            Seq(
              ReportFields(
                key = Some(simpleProduct1.id.toString),
                values = ProductSales(simpleProduct1, OrderItemSalesAggregate(count = 1, quantity = Some(4.500))),
              ),
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
