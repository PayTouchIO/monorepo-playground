package io.paytouch.core.reports.async.exporters

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ Pagination, UserContext, VariantOptionWithType }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportListFilters
import io.paytouch.core.reports.views._
import io.paytouch.core.utils.{ MockedRestApi, UtcTime }

class ProductSalesListCSVConverterSpec extends CSVConverterHelperSpec {
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

  "CSVConverterHelper for ReportResponse[ProductSales]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val productId = UUID.randomUUID

      val variantOptionWithTypes = Seq(
        VariantOptionWithType(UUID.randomUUID, "my option A", "my option type A", position = 1, typePosition = 1),
        VariantOptionWithType(UUID.randomUUID, "my option B", "my option type B", position = 2, typePosition = 1),
      )

      val entity: ReportResponse[ProductSales] =
        reportResponseBuilder(
          Seq(
            ProductSales(
              productId,
              "My Beautiful Product",
              None,
              Some("my upc"),
              Some(ZonedDateTime.parse("2018-03-23T11:45:27.832Z")),
              Some(variantOptionWithTypes),
              orderItemSales,
            ),
          ),
        )

      @scala.annotation.nowarn("msg=Auto-application")
      implicit val userContext = random[UserContext]
      implicit val pagination = random[Pagination]

      implicit val converterHelper = converterBuilder(productSalesConverter)

      val productSalesView = ProductSalesView(MockedRestApi.variantService)
      val filters = ReportListFilters[ProductSalesView](
        productSalesView,
        startDate,
        endDate,
        None,
        ProductSalesOrderByFields.values,
        None,
        None,
        None,
        None,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 1
      header ==== List(
        "Product ID",
        "Product Name",
        "SKU",
        "UPC",
        "Deleted at",
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
        productId.toString,
        "My Beautiful Product - my option A - my option B",
        "",
        "my upc",
        "2018-03-23T11:45:27.832Z",
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

      val productId = UUID.randomUUID

      val entity: ReportResponse[ProductSales] =
        reportResponseBuilder(
          Seq(ProductSales(productId, "My Beautiful Product", None, Some("my upc"), None, None, orderItemSales)),
        )

      @scala.annotation.nowarn("msg=Auto-application")
      implicit val userContext = random[UserContext]
      implicit val pagination = random[Pagination]

      implicit val converterHelper = converterBuilder(productSalesConverter)

      val productSalesView = ProductSalesView(MockedRestApi.variantService)
      val filters = ReportListFilters[ProductSalesView](
        productSalesView,
        startDate,
        endDate,
        None,
        ProductSalesOrderByFields.values.filterNot(_ == ProductSalesOrderByFields.Count),
        None,
        None,
        None,
        None,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 1
      header ==== List(
        "Product ID",
        "Product Name",
        "SKU",
        "UPC",
        "Deleted at",
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
        productId.toString,
        "My Beautiful Product",
        "",
        "my upc",
        "",
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
