package io.paytouch.core.reports.async.exporters

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportAggrFilters
import io.paytouch.core.reports.views._

class ReportFieldsSalesAggregateCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[ReportFields[SalesAggregate]]" should {

    val semiEmptyAggregate =
      SalesAggregate(5, Some(13.$$$), None, None, None, None, None, None, None, None, Some(10.$$$))
    val fullAggregate = SalesAggregate(
      1,
      Some(0.5.$$$),
      Some(1.$$$),
      Some(2.$$$),
      Some(3.$$$),
      Some(4.$$$),
      Some(5.$$$),
      Some(6.$$$),
      Some(7.$$$),
      Some(8.$$$),
      Some(9.$$$),
      Some(10.$$$),
    )

    "convert instances of report response" in new CSVConverterHelperSpecContext {
      val entity: ReportResponse[ReportFields[SalesAggregate]] = reportResponseBuilder(
        Seq(ReportFields(None, fullAggregate)),
        Seq(ReportFields(None, semiEmptyAggregate)),
      )
      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(salesAggregateConverter))
      val filters = ReportAggrFilters[SalesView](
        SalesView(),
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        None,
        SalesFields.values,
        None,
        None,
        merchantId,
      )

      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 2
      header ==== List(
        "Start Time",
        "End Time",
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
        "Tender - cash",
        "Tender - credit card",
        "Tender - debit card",
        "Tender - gift card",
        "Tips",
      )
      rows.head ==== List(
        "2015-12-03T20:15:30",
        "2016-01-03T20:15:30",
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
        "0.00",
        "0.00",
        "0.00",
        "0.00",
        "10.00",
      )
      rows(1) ==== List(
        "2015-12-04T20:15:30",
        "2016-01-04T20:15:30",
        "13.00",
        "5",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "10.00",
        "0.00",
        "0.00",
        "0.00",
        "0.00",
        "",
      )
    }

    "convert instances of report response without count" in new CSVConverterHelperSpecContext {
      val entity: ReportResponse[ReportFields[SalesAggregate]] = reportResponseBuilder(
        Seq(ReportFields(None, fullAggregate)),
        Seq(ReportFields(None, semiEmptyAggregate)),
      )
      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(salesAggregateConverter))
      val filters = ReportAggrFilters[SalesView](
        SalesView(),
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        None,
        SalesFields.values.filterNot(_ == SalesFields.Count),
        None,
        None,
        merchantId,
      )

      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 2
      header ==== List(
        "Start Time",
        "End Time",
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
        "Tender - cash",
        "Tender - credit card",
        "Tender - debit card",
        "Tender - gift card",
        "Tips",
      )
      rows.head ==== List(
        "2015-12-03T20:15:30",
        "2016-01-03T20:15:30",
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
        "0.00",
        "0.00",
        "0.00",
        "0.00",
        "10.00",
      )
      rows(1) ==== List(
        "2015-12-04T20:15:30",
        "2016-01-04T20:15:30",
        "13.00",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "10.00",
        "0.00",
        "0.00",
        "0.00",
        "0.00",
        "",
      )
    }
  }

}
