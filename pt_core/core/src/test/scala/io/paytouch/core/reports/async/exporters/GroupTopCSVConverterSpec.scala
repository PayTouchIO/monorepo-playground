package io.paytouch.core.reports.async.exporters

import java.util.UUID

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.entities.enums.ops.GroupOrderByFields
import io.paytouch.core.reports.filters.ReportTopFilters
import io.paytouch.core.reports.views._

class GroupTopCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[GroupTop]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val groupId = UUID.randomUUID

      val entity: ReportResponse[GroupTop] = reportResponseBuilder(
        Seq(GroupTop(groupId, "my_group", 5.$$$, 10.$$$, 32.2, 1)),
      )

      implicit val converterHelper = converterBuilder(groupTopConverter)
      val filters = ReportTopFilters[GroupView](
        GroupView,
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        GroupOrderByFields.values,
        5,
        merchantId,
      )

      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 1
      header ==== List("Start Time", "End Time", "Group ID", "Name", "Spend", "Profit", "Margin", "Visits")
      rows.head ==== List(
        "2015-12-03T20:15:30",
        "2016-01-03T20:15:30",
        groupId.toString,
        "my_group",
        "5.00",
        "10.00",
        "32.20",
        "1",
      )

    }
  }

}
