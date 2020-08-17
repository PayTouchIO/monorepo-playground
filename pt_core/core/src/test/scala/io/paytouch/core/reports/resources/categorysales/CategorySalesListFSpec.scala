package io.paytouch.core.reports.resources.categorysales

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.Pagination
import io.paytouch.core.reports.entities.{ CategorySales, OrderItemSalesAggregate }
import org.specs2.specification.core.Fragment

class CategorySalesListFSpec extends CategorySalesFSpec {

  import fixtures._

  val emptyCategory1Aggregate = OrderItemSalesAggregate(count = 5)
  val fullCategory1Aggregate = emptyCategory1Aggregate.copy(
    discounts = Some(7.2.$$$),
    grossProfits = Some(-1.2.$$$),
    grossSales = Some(16.00.$$$),
    margin = Some(-10.00),
    netSales = Some(12.00.$$$),
    quantity = Some(13.50),
    returnedAmount = Some(41.00.$$$),
    returnedQuantity = Some(4.00),
    cost = Some(13.20.$$$),
    taxable = Some(11.0.$$$),
    nonTaxable = Some(1.$$$),
    taxes = Some(4.$$$),
  )

  val emptyCategory2Aggregate = OrderItemSalesAggregate(count = 2)
  val fullCategory2Aggregate = emptyCategory2Aggregate.copy(
    discounts = Some(0.$$$),
    grossProfits = Some(0.$$$),
    grossSales = Some(0.$$$),
    margin = Some(0),
    netSales = Some(0.$$$),
    quantity = Some(0.00),
    returnedAmount = Some(1.00.$$$),
    returnedQuantity = Some(1.00),
    cost = Some(0.$$$),
    taxable = Some(0.$$$),
    nonTaxable = Some(0.$$$),
    taxes = Some(0.$$$),
  )

  val resultOrdered12 = Seq(
    CategorySales(fixtures.category1, fullCategory1Aggregate),
    CategorySales(fixtures.category2, fullCategory2Aggregate),
  )

  val resultOrdered21 = Seq(
    CategorySales(fixtures.category2, fullCategory2Aggregate),
    CategorySales(fixtures.category1, fullCategory1Aggregate),
  )
  val emptyResultOrdered12 = Seq(
    CategorySales(fixtures.category1, emptyCategory1Aggregate),
    CategorySales(fixtures.category2, emptyCategory2Aggregate),
  )

  val totalCount = 2

  "GET /v1/reports/category_sales.list" in {
    "with default order by name" should {
      assertNoField()

      def assertFieldResultWithPaginationAndDefaults(
          field: String,
          f: (OrderItemSalesAggregate, OrderItemSalesAggregate) => OrderItemSalesAggregate,
        ): Fragment = {
        val expectedResult = emptyResultOrdered12.zip(resultOrdered12).map {
          case (empty, full) =>
            empty.copy(data = f(empty.data, full.data))
        }
        assertFieldResultWithPagination(field, totalCount, expectedResult)
      }

      assertFieldResultWithPaginationAndDefaults("discounts", (empty, full) => empty.copy(discounts = full.discounts))

      assertFieldResultWithPaginationAndDefaults(
        "gross_profits",
        (empty, full) => empty.copy(grossProfits = full.grossProfits),
      )

      assertFieldResultWithPaginationAndDefaults(
        "gross_sales",
        (empty, full) => empty.copy(grossSales = full.grossSales),
      )

      assertFieldResultWithPaginationAndDefaults("margin", (empty, full) => empty.copy(margin = full.margin))

      assertFieldResultWithPaginationAndDefaults("net_sales", (empty, full) => empty.copy(netSales = full.netSales))

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

      assertAllFieldsResultWithPagination(totalCount, resultOrdered12)
    }

    "with custom order" should {

      assertOrderByResultWithPagination("name", totalCount, resultOrdered12)

      assertOrderByResultWithPagination("discounts", totalCount, resultOrdered12)

      assertOrderByResultWithPagination("gross_sales", totalCount, resultOrdered12)

      assertOrderByResultWithPagination("quantity", totalCount, resultOrdered12)

      assertOrderByResultWithPagination("returned_quantity", totalCount, resultOrdered12)

      assertOrderByResultWithPagination("returned_amount", totalCount, resultOrdered12)

      assertOrderByResultWithPagination("count", totalCount, resultOrdered12)

      assertOrderByResultWithPagination("cost", totalCount, resultOrdered12)

      assertOrderByResultWithPagination("taxable", totalCount, resultOrdered12)

      assertOrderByResultWithPagination("non_taxable", totalCount, resultOrdered12)

      assertOrderByResultWithPagination("taxes", totalCount, resultOrdered12)
    }

    "with pagination" should {
      assertAllFieldsResultWithPagination(
        totalCount,
        Seq(CategorySales(fixtures.category1, fullCategory1Aggregate)),
        pagination = Some(Pagination(1, 1)),
      )

    }

    "filtered by id[]" should {
      assertAllFieldsResultWithPagination(
        1,
        Seq(CategorySales(fixtures.category2, fullCategory2Aggregate)),
        extraFilters = Some(s"id[]=${fixtures.category2.id}"),
      )
    }

    "filtered by category_id[]" should {
      assertAllFieldsResultWithPagination(
        1,
        Seq(CategorySales(fixtures.category2, fullCategory2Aggregate)),
        extraFilters = Some(s"category_id[]=${fixtures.category2.id}"),
      )
    }

    "filtered by location_id" should {
      assertAllFieldsResultWithPagination(
        2,
        Seq(
          CategorySales(
            fixtures.category1,
            OrderItemSalesAggregate(
              count = 1,
              discounts = Some(1.5.$$$),
              grossProfits = Some(-2.2.$$$),
              grossSales = Some(5.00.$$$),
              margin = Some(-95.6522),
              netSales = Some(2.3.$$$),
              quantity = Some(4.5000),
              returnedAmount = Some(0.0.$$$),
              returnedQuantity = Some(0),
              cost = Some(4.5.$$$),
              taxable = Some(2.30.$$$),
              nonTaxable = Some(0.$$$),
              taxes = Some(2.7.$$$),
            ),
          ),
          CategorySales(
            fixtures.category2,
            OrderItemSalesAggregate(
              count = 0,
              discounts = Some(0.$$$),
              grossProfits = Some(0.$$$),
              grossSales = Some(0.$$$),
              margin = Some(0),
              netSales = Some(0.$$$),
              quantity = Some(0),
              returnedAmount = Some(0.00.$$$),
              returnedQuantity = Some(0.0),
              cost = Some(0.$$$),
              taxable = Some(0.$$$),
              nonTaxable = Some(0.$$$),
              taxes = Some(0.$$$),
            ),
          ),
        ),
        extraFilters = Some(s"location_id=${london.id}"),
      )
    }
  }
}
