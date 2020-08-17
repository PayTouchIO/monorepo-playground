package io.paytouch.core.reports.async.exporters

import java.util.UUID

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ Pagination, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportAggrFilters
import io.paytouch.core.reports.views._

class LocationGiftCardPassAggrCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[LocationGiftCardPasses]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val locationId = UUID.randomUUID

      val entity: ReportResponse[ReportFields[LocationGiftCardPasses]] = {
        val passes = GiftCardPassAggregate(1, Some(2), Some(0.5.$$$), Some(1.$$$), Some(2.$$$))

        reportResponseBuilder(
          Seq(
            ReportFields(
              Some(locationId.toString),
              LocationGiftCardPasses(locationId, "London", Some("1 Buckingham Palace"), passes),
            ),
          ),
        )
      }

      @scala.annotation.nowarn("msg=Auto-application")
      implicit val userContext = random[UserContext]
      implicit val pagination = random[Pagination]

      implicit val converterHelper = converterBuilder(new ReportFieldsConverter(locationGiftCardPassesConverter))
      val filters = ReportAggrFilters[LocationGiftCardPassView](
        LocationGiftCardPassView(),
        startDate,
        endDate,
        locationIds,
        ReportInterval.Daily,
        None,
        LocationGiftCardPassFields.values,
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
        "Count",
        "Customers",
        "Total Value",
        "Total Redeemed",
        "Total Unused",
      )

      rows.head ==== List(
        "2015-12-03T20:15:30",
        "2016-01-03T20:15:30",
        "London - 1 Buckingham Palace",
        locationId.toString,
        "1",
        "2",
        "0.50",
        "1.00",
        "2.00",
      )
    }
  }

}
