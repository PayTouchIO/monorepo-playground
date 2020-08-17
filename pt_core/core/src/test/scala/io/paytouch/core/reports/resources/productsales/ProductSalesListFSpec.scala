package io.paytouch.core.reports.resources.productsales

import io.paytouch.core.entities.Pagination
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.entities.{ OrderItemSalesAggregate, ProductSales }
import io.paytouch.core.reports.resources.ReportsListFSpec
import io.paytouch.core.reports.views.ProductSalesView
import io.paytouch.core.utils.MockedRestApi
import org.specs2.specification.core.Fragment

class ProductSalesListFSpec extends ReportsListFSpec[ProductSalesView] {

  class ProductSalesListFSpecContext extends ReportsListFSpecContext with ProductSalesFSpecFixtures

  val view = ProductSalesView(MockedRestApi.variantService)

  val fixtures = new ProductSalesListFSpecContext
  import fixtures._

  "GET /v1/reports/product_sales.list" in {
    "with default order by name" should {
      assertNoField()

      def assertFieldResultWithPaginationAndDefaults(
          field: String,
          f: (OrderItemSalesAggregate, OrderItemSalesAggregate) => OrderItemSalesAggregate,
        ): Fragment = {
        val expectedResult = emptyResultOrdered123AB.zip(resultOrdered123AB).map {
          case (empty, full) =>
            empty.copy(data = f(empty.data, full.data))
        }
        assertFieldResultWithPagination(field, totalCount, expectedResult)
      }

      assertFieldResultWithPaginationAndDefaults("discounts", (empty, full) => empty.copy(discounts = full.discounts))

      assertFieldResultWithPaginationAndDefaults(
        "gross_sales",
        (empty, full) => empty.copy(grossSales = full.grossSales),
      )

      assertFieldResultWithPaginationAndDefaults("quantity", (empty, full) => empty.copy(quantity = full.quantity))

      assertFieldResultWithPaginationAndDefaults(
        "returned_quantity",
        (empty, full) => empty.copy(returnedQuantity = full.returnedQuantity),
      )

      assertFieldResultWithPaginationAndDefaults(
        "returned_amount",
        (empty, full) => empty.copy(returnedAmount = full.returnedAmount),
      )

      assertFieldResultWithPaginationAndDefaults("cost", (empty, full) => empty.copy(cost = full.cost))

      assertFieldResultWithPaginationAndDefaults("taxable", (empty, full) => empty.copy(taxable = full.taxable))

      assertFieldResultWithPaginationAndDefaults(
        "non_taxable",
        (empty, full) => empty.copy(nonTaxable = full.nonTaxable),
      )

      assertFieldResultWithPaginationAndDefaults("taxes", (empty, full) => empty.copy(taxes = full.taxes))

      assertAllFieldsResultWithPagination(totalCount, resultOrdered123AB)
    }

    "with custom order" should {

      assertOrderByResultWithPagination("name", totalCount, resultOrdered123AB)

      assertOrderByResultWithPagination("upc", totalCount, resultOrdered312AB)

      assertOrderByResultWithPagination("sku", totalCount, resultOrdered321AB)

      assertOrderByResultWithPagination("discounts", totalCount, resultOrdered123AB)

      assertOrderByResultWithPagination("gross_sales", totalCount, resultOrdered123AB)

      assertOrderByResultWithPagination("quantity", totalCount, resultOrdered123AB)

      assertOrderByResultWithPagination("returned_quantity", totalCount, resultOrdered123AB)

      assertOrderByResultWithPagination("returned_amount", totalCount, resultOrdered123AB)

      assertOrderByResultWithPagination("count", totalCount, resultOrdered123AB)

      assertOrderByResultWithPagination("cost", totalCount, resultOrdered123AB)

      assertOrderByResultWithPagination("taxable", totalCount, resultOrdered123AB)

      assertOrderByResultWithPagination("non_taxable", totalCount, resultOrdered123AB)

      assertOrderByResultWithPagination("taxes", totalCount, resultOrdered123AB)
    }

    "with pagination" should {
      assertAllFieldsResultWithPagination(
        totalCount,
        Seq(ProductSales(fixtures.simpleProduct1, fullProduct1Aggregate)),
        pagination = Some(Pagination(1, 1)),
      )

    }

    "filtered by id[]" should {
      assertAllFieldsResultWithPagination(
        1,
        Seq(ProductSales(fixtures.variantProduct3, fullProduct3Aggregate)),
        extraFilters = Some(s"id[]=${fixtures.variantProduct3.id}"),
      )
    }

    "filtered by category_id[]" should {
      assertAllFieldsResultWithPagination(
        1,
        Seq(ProductSales(fixtures.simpleProduct1, fullProduct1Aggregate)),
        extraFilters = Some(s"category_id[]=${fixtures.category1.id}"),
      )
    }

    "filtered by location_id" should {
      assertAllFieldsResultWithPagination(
        totalCount,
        Seq(
          ProductSales(
            fixtures.simpleProduct1,
            OrderItemSalesAggregate(
              count = 1,
              discounts = Some(1.5.$$$),
              grossProfits = Some(-2.2.$$$),
              grossSales = Some(5.0.$$$),
              margin = Some(-95.6522),
              netSales = Some(2.3.$$$),
              quantity = Some(4.500),
              returnedAmount = Some(0.$$$),
              returnedQuantity = Some(0),
              cost = Some(4.5.$$$),
              taxable = Some(2.3.$$$),
              nonTaxable = Some(0.$$$),
              taxes = Some(2.7.$$$),
            ),
          ),
        ),
        extraFilters = Some(s"location_id=${london.id}"),
        pagination = Some(Pagination(1, 1)),
      )
    }
  }
}
