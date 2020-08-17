package io.paytouch.core.reports.async.exporters

import java.time.LocalDateTime
import java.util.UUID

import org.specs2.specification.Scope

import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.services.VariantService
import io.paytouch.core.utils.PaytouchSpec

abstract class CSVConverterHelperSpec extends PaytouchSpec {
  @scala.annotation.nowarn("msg=Auto-application")
  implicit val merchantContext = random[UserContext].toMerchantContext

  abstract class CSVConverterHelperSpecContext extends Scope {
    val startDate = LocalDateTime.parse("2015-12-03T20:15:30")
    val endDate = startDate.plusMonths(1)

    val mockVariantService = mock[VariantService]

    val merchantId = UUID.randomUUID
    val locationIds = Seq(UUID.randomUUID)

    val converter = new CSVConverter

    def reportResponseBuilder[T](data: Seq[T]*): ReportResponse[T] = {
      val metadata = ReportMetadata(None, None)
      ReportResponse(metadata, data.zipWithIndex.map { case (d, idx) => reportDataBuilder(d, idx) })
    }

    private def reportDataBuilder[T](data: Seq[T], idx: Int): ReportData[T] = {
      val timeframe = ReportTimeframe(startDate.plusDays(idx), endDate.plusDays(idx))
      ReportData(timeframe, data)
    }

    def toCSV[T](
        data: ReportResponse[T],
        f: ReportFilters,
      )(implicit
        helper: CSVConverterHelper[ReportResponse[T]],
      ): (List[String], List[List[String]]) = {
      val (header, rows) = converter.convertToData(data, f).await
      val cleanData = CSVConverterHelper.removeIgnorableColumns(List(header) ++ rows)
      (cleanData.head, cleanData.tail)
    }
  }

}
