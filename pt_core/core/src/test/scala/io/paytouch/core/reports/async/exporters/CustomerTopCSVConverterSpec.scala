package io.paytouch.core.reports.async.exporters

import java.util.UUID

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportTopFilters
import io.paytouch.core.reports.views._

class CustomerTopCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[CustomerTop]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val customerId = UUID.randomUUID

      val entity: ReportResponse[CustomerTop] = reportResponseBuilder(
        Seq(CustomerTop(customerId, None, Some("Sfregola"), 5.$$$, 10.$$$, 32.2, 1)),
      )

      implicit val converterHelper = converterBuilder(customerTopConverter)
      val filters = ReportTopFilters[CustomerView](
        CustomerView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        CustomerOrderByFields.values,
        5,
        merchantId,
      )

      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 1
      header ==== List(
        "Start Time",
        "End Time",
        "Customer ID",
        "First Name",
        "Last Name",
        "Profit",
        "Spend",
        "Margin",
        "Visits",
      )
      rows.head ==== List(
        "2015-12-03T20:15:30",
        "2016-01-03T20:15:30",
        customerId.toString,
        "",
        "Sfregola",
        "5.00",
        "10.00",
        "32.20",
        "1",
      )
    }

  }
}
