package io.paytouch.core.reports.async.exporters

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportAggrFilters
import io.paytouch.core.reports.views._

class ReportFieldsGiftCardPassAggregateCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[ReportFields[GiftCardPassAggregate]]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val entity: ReportResponse[ReportFields[GiftCardPassAggregate]] = reportResponseBuilder(
        Seq(
          ReportFields(
            Some("key"),
            GiftCardPassAggregate(1, customers = Some(1), Some(10.$$$), Some(11.$$$), Some(12.$$$)),
          ),
        ),
        Seq(
          ReportFields(
            Some("another_key"),
            GiftCardPassAggregate(1, customers = Some(2), Some(13.$$$), Some(14.$$$), Some(15.$$$)),
          ),
          ReportFields(Some("another_another_key"), GiftCardPassAggregate(5)),
        ),
      )

      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(giftCardPassAggregateConverter))
      val filters = ReportAggrFilters[GiftCardPassView](
        GiftCardPassView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        Some(GiftCardPassGroupBy.Value),
        GiftCardPassFields.values,
        None,
        None,
        merchantId,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 3
      header ==== List(
        "Start Time",
        "End Time",
        "Value",
        "Count",
        "Customers",
        "Total Value",
        "Total Redeemed",
        "Total Unused",
      )
      rows.head ==== List("2015-12-03T20:15:30", "2016-01-03T20:15:30", "key", "1", "1", "10.00", "11.00", "12.00")
      rows(1) ==== List(
        "2015-12-04T20:15:30",
        "2016-01-04T20:15:30",
        "another_key",
        "1",
        "2",
        "13.00",
        "14.00",
        "15.00",
      )
      rows(2) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another_another_key", "5", "", "", "", "")
    }

    "convert instances of report response without a count" in new CSVConverterHelperSpecContext {

      val entity: ReportResponse[ReportFields[GiftCardPassAggregate]] = reportResponseBuilder(
        Seq(
          ReportFields(
            Some("key"),
            GiftCardPassAggregate(1, customers = Some(1), Some(10.$$$), Some(11.$$$), Some(12.$$$)),
          ),
        ),
        Seq(
          ReportFields(
            Some("another_key"),
            GiftCardPassAggregate(1, customers = Some(2), Some(13.$$$), Some(14.$$$), Some(15.$$$)),
          ),
          ReportFields(Some("another_another_key"), GiftCardPassAggregate(5)),
        ),
      )

      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(giftCardPassAggregateConverter))
      val filters = ReportAggrFilters[GiftCardPassView](
        GiftCardPassView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        Some(GiftCardPassGroupBy.Value),
        GiftCardPassFields.values.filterNot(_ == GiftCardPassFields.Count),
        None,
        None,
        merchantId,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 3
      header ==== List("Start Time", "End Time", "Value", "Customers", "Total Value", "Total Redeemed", "Total Unused")
      rows.head ==== List("2015-12-03T20:15:30", "2016-01-03T20:15:30", "key", "1", "10.00", "11.00", "12.00")
      rows(1) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another_key", "2", "13.00", "14.00", "15.00")
      rows(2) ==== List("2015-12-04T20:15:30", "2016-01-04T20:15:30", "another_another_key", "", "", "", "")
    }
  }

}
