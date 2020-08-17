package io.paytouch.core.reports.async.exporters

import java.util.UUID

import io.paytouch.core.data.model.enums.TransactionPaymentType
import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ Pagination, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportAggrFilters
import io.paytouch.core.reports.views._

class LocationSalesAggrCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[LocationSales]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val locationId = UUID.randomUUID

      val entity: ReportResponse[ReportFields[LocationSales]] = {
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
            tips = Some(12.$$$),
            tenderTypes =
              Some(Map(TransactionPaymentType.Cash -> (10.$$$), TransactionPaymentType.GiftCard -> (11.$$$))),
          )

        reportResponseBuilder(
          Seq(
            ReportFields(
              Some(locationId.toString),
              LocationSales(locationId, "London", Some("1 Buckingham Palace"), sales),
            ),
          ),
        )
      }

      @scala.annotation.nowarn("msg=Auto-application")
      implicit val userContext = random[UserContext]
      implicit val pagination = random[Pagination]

      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(locationSalesConverter))
      val filters = ReportAggrFilters[LocationSalesView](
        LocationSalesView(),
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        None,
        LocationSalesFields.values,
        None,
        None,
        merchantId,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 1
      header ==== List(
        "Start Time",
        "End Time",
        "Location Name",
        "Location ID",
        "Number of Sales",
        "Gift Card Sales",
        "Profits",
        "Gross Sales",
        "Net Sales",
        "Non-Taxable Sales",
        "Taxable Sales",
        "COGS",
        "Discounts",
        "Refunds",
        "Tax Collected",
        "Tips",
        "Tender - cash",
        "Tender - credit card",
        "Tender - debit card",
        "Tender - gift card",
      )

      rows.head ==== List(
        "2015-12-03T20:15:30",
        "2016-01-03T20:15:30",
        "London - 1 Buckingham Palace",
        locationId.toString,
        "1",
        "2.00",
        "3.00",
        "4.00",
        "5.00",
        "6.00",
        "8.00",
        "0.50",
        "1.00",
        "7.00",
        "9.00",
        "12.00",
        "10.00",
        "0.00",
        "0.00",
        "11.00",
      )
    }
  }

}
