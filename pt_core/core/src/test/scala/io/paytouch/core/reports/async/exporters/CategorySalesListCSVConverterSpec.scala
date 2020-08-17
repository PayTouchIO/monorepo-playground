package io.paytouch.core.reports.async.exporters

import java.util.UUID

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ Pagination, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportListFilters
import io.paytouch.core.reports.views._

class CategorySalesListCSVConverterSpec extends CSVConverterHelperSpec {
  val orderItemSales =
    OrderItemSalesAggregate(
      count = 1,
      discounts = Some(2.$$$),
      grossProfits = Some(11.$$$),
      grossSales = Some(3.$$$),
      margin = Some(12),
      netSales = Some(13.$$$),
      quantity = Some(4),
      returnedAmount = Some(5.$$$),
      returnedQuantity = Some(6),
      cost = Some(7.$$$),
      taxable = Some(8.$$$),
      nonTaxable = Some(9.$$$),
      taxes = Some(10.$$$),
    )

  "CSVConverterHelper for ReportResponse[CategorySales]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val categoryId = UUID.randomUUID

      val entity: ReportResponse[CategorySales] =
        reportResponseBuilder(Seq(CategorySales(categoryId, "My Beautiful Category", orderItemSales)))

      @scala.annotation.nowarn("msg=Auto-application")
      implicit val userContext = random[UserContext]
      implicit val pagination = random[Pagination]

      implicit val converterHelper = converterBuilder(categorySalesConverter)
      val filters = ReportListFilters[CategorySalesView](
        CategorySalesView,
        startDate,
        endDate,
        None,
        CategorySalesOrderByFields.values,
        None,
        None,
        None,
        None,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 1
      header ==== List(
        "Category ID",
        "Category Name",
        "Count",
        "COGS",
        "Discounts",
        "Gross Profits",
        "Gross Sales",
        "Margin",
        "Net Sales",
        "Quantity Sold",
        "Returned Quantity",
        "Returned Amount",
        "Tax Collected",
        "Taxable Sales",
        "Non-Taxable Sales",
      )

      rows.head ==== List(
        categoryId.toString,
        "My Beautiful Category",
        "1",
        "7.00",
        "2.00",
        "11.00",
        "3.00",
        "12.00",
        "13.00",
        "4.00",
        "6.00",
        "5.00",
        "10.00",
        "8.00",
        "9.00",
      )
    }

    "convert instances of report response without count" in new CSVConverterHelperSpecContext {

      val categoryId = UUID.randomUUID

      val entity: ReportResponse[CategorySales] =
        reportResponseBuilder(Seq(CategorySales(categoryId, "My Beautiful Category", orderItemSales)))

      @scala.annotation.nowarn("msg=Auto-application")
      implicit val userContext = random[UserContext]
      implicit val pagination = random[Pagination]

      implicit val converterHelper = converterBuilder(categorySalesConverter)
      val filters = ReportListFilters[CategorySalesView](
        CategorySalesView,
        startDate,
        endDate,
        None,
        CategorySalesOrderByFields.values.filterNot(_ == CategorySalesOrderByFields.Count),
        None,
        None,
        None,
        None,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 1
      header ==== List(
        "Category ID",
        "Category Name",
        "COGS",
        "Discounts",
        "Gross Profits",
        "Gross Sales",
        "Margin",
        "Net Sales",
        "Quantity Sold",
        "Returned Quantity",
        "Returned Amount",
        "Tax Collected",
        "Taxable Sales",
        "Non-Taxable Sales",
      )

      rows.head ==== List(
        categoryId.toString,
        "My Beautiful Category",
        "7.00",
        "2.00",
        "11.00",
        "3.00",
        "12.00",
        "13.00",
        "4.00",
        "6.00",
        "5.00",
        "10.00",
        "8.00",
        "9.00",
      )
    }
  }

}
