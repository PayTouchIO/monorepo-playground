package io.paytouch.core.reports.async.exporters

import java.util.UUID

import io.paytouch.core.data.model.enums.TransactionPaymentType
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ Pagination, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportListFilters
import io.paytouch.core.reports.views._

class LocationSalesListCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[LocationSales]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val locationId = UUID.randomUUID

      val entity: ReportResponse[LocationSales] = {
        val sales =
          SalesAggregate(
            count = 1,
            costs = Some(0.5.$$$),
            discounts = Some(1.$$$),
            giftCardSales = Some(2.$$$),
            grossProfits = Some(3.$$$),
            grossSales = Some(4.$$$),
            netSales = Some(5.$$$),
            nonTaxable = Some(6.$$$),
            refunds = Some(7.$$$),
            taxable = Some(8.$$$),
            taxes = Some(9.$$$),
            tips = Some(11.$$$),
            tenderTypes =
              Some(Map(TransactionPaymentType.Cash -> (10.$$$), TransactionPaymentType.GiftCard -> (11.$$$))),
          )
        reportResponseBuilder(Seq(LocationSales(locationId, "London", Some("1 Buckingham Palace"), sales)))
      }

      @scala.annotation.nowarn("msg=Auto-application")
      implicit val userContext = random[UserContext]
      implicit val pagination = random[Pagination]

      implicit val converterHelper = converterBuilder(locationSalesConverter)
      val filters = ReportListFilters[LocationSalesView](
        LocationSalesView(),
        startDate,
        endDate,
        None,
        LocationSalesOrderByFields.values,
        None,
        None,
        None,
        None,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 1
      header ==== List(
        "Location ID",
        "Location Name",
        "COGS",
        "Number of Sales",
        "Discounts",
        "Gift Card Sales",
        "Profits",
        "Gross Sales",
        "Net Sales",
        "Non-Taxable Sales",
        "Refunds",
        "Taxable Sales",
        "Tax Collected",
        "Tips",
        "Tender - cash",
        "Tender - credit card",
        "Tender - debit card",
        "Tender - gift card",
      )

      rows.head ==== List(
        locationId.toString,
        "London - 1 Buckingham Palace",
        "0.50",
        "1",
        "1.00",
        "2.00",
        "3.00",
        "4.00",
        "5.00",
        "6.00",
        "7.00",
        "8.00",
        "9.00",
        "11.00",
        "10.00",
        "0.00",
        "0.00",
        "11.00",
      )
    }

    "convert instances of report response without count" in new CSVConverterHelperSpecContext {

      val locationId = UUID.randomUUID

      val entity: ReportResponse[LocationSales] = {
        val sales =
          SalesAggregate(
            count = 1,
            costs = Some(0.5.$$$),
            discounts = Some(1.$$$),
            giftCardSales = Some(2.$$$),
            grossProfits = Some(3.$$$),
            grossSales = Some(4.$$$),
            netSales = Some(5.$$$),
            nonTaxable = Some(6.$$$),
            refunds = Some(7.$$$),
            taxable = Some(8.$$$),
            taxes = Some(9.$$$),
            tips = Some(10.$$$),
          )
        reportResponseBuilder(Seq(LocationSales(locationId, "London", Some("1 Buckingham Palace"), sales)))
      }

      @scala.annotation.nowarn("msg=Auto-application")
      implicit val userContext = random[UserContext]
      implicit val pagination = random[Pagination]

      implicit val converterHelper = converterBuilder(locationSalesConverter)
      val filters = ReportListFilters[LocationSalesView](
        LocationSalesView(),
        startDate,
        endDate,
        None,
        LocationSalesOrderByFields.values.filterNot(_ == LocationSalesOrderByFields.Count),
        None,
        None,
        None,
        None,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 1
      header ==== List(
        "Location ID",
        "Location Name",
        "COGS",
        "Discounts",
        "Gift Card Sales",
        "Profits",
        "Gross Sales",
        "Net Sales",
        "Non-Taxable Sales",
        "Refunds",
        "Taxable Sales",
        "Tax Collected",
        "Tips",
        "Tender - cash",
        "Tender - credit card",
        "Tender - debit card",
        "Tender - gift card",
      )

      rows.head ==== List(
        locationId.toString,
        "London - 1 Buckingham Palace",
        "0.50",
        "1.00",
        "2.00",
        "3.00",
        "4.00",
        "5.00",
        "6.00",
        "7.00",
        "8.00",
        "9.00",
        "10.00",
        "0.00",
        "0.00",
        "0.00",
        "0.00",
      )
    }
  }

}
