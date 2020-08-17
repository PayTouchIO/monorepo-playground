package io.paytouch.core.reports.async.exporters

import java.util.UUID

import io.paytouch.core.entities.MonetaryAmount._
import io.paytouch.core.entities.{ Pagination, UserContext }
import io.paytouch.core.reports.async.exporters.CSVConverterHelperInstances._
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums._
import io.paytouch.core.reports.filters.ReportListFilters
import io.paytouch.core.reports.views._

class LocationGiftCardPassListCSVConverterSpec extends CSVConverterHelperSpec {

  "CSVConverterHelper for ReportResponse[LocationGiftCardPasses]" should {

    "convert instances of report response" in new CSVConverterHelperSpecContext {

      val locationId = UUID.randomUUID

      val entity: ReportResponse[LocationGiftCardPasses] = {
        val passes = GiftCardPassAggregate(1, Some(2), Some(0.5.$$$), Some(1.$$$), Some(2.$$$))
        reportResponseBuilder(Seq(LocationGiftCardPasses(locationId, "London", Some("1 Buckingham Palace"), passes)))
      }

      @scala.annotation.nowarn("msg=Auto-application")
      implicit val userContext = random[UserContext]
      implicit val pagination = random[Pagination]

      implicit val converterHelper = converterBuilder(locationGiftCardPassesConverter)
      val filters = ReportListFilters[LocationGiftCardPassView](
        LocationGiftCardPassView(),
        startDate,
        endDate,
        None,
        LocationGiftCardPassesOrderByFields.values,
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
        "Count",
        "Customers",
        "Total Value",
        "Total Redeemed",
        "Total Unused",
      )
      rows.head ==== List(locationId.toString, "London - 1 Buckingham Palace", "1", "2", "0.50", "1.00", "2.00")
    }

    "convert instances of report response without count" in new CSVConverterHelperSpecContext {

      val locationId = UUID.randomUUID

      val entity: ReportResponse[LocationGiftCardPasses] = {
        val passes = GiftCardPassAggregate(1, Some(2), Some(0.5.$$$), Some(1.$$$), Some(2.$$$))
        reportResponseBuilder(Seq(LocationGiftCardPasses(locationId, "London", Some("1 Buckingham Palace"), passes)))
      }

      @scala.annotation.nowarn("msg=Auto-application")
      implicit val userContext = random[UserContext]
      implicit val pagination = random[Pagination]

      implicit val converterHelper = converterBuilder(locationGiftCardPassesConverter)
      val filters = ReportListFilters[LocationGiftCardPassView](
        LocationGiftCardPassView(),
        startDate,
        endDate,
        None,
        LocationGiftCardPassesOrderByFields.values.filterNot(_ == LocationGiftCardPassesOrderByFields.Count),
        None,
        None,
        None,
        None,
      )
      val (header, rows) = toCSV(entity, filters)

      rows.size ==== 1
      header ==== List("Location ID", "Location Name", "Customers", "Total Value", "Total Redeemed", "Total Unused")
      rows.head ==== List(locationId.toString, "London - 1 Buckingham Palace", "2", "0.50", "1.00", "2.00")
    }
  }

}
