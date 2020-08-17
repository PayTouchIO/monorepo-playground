package io.paytouch.core.reports.resources

import java.time.{ LocalDateTime, ZonedDateTime }

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingQueryParamRejection
import akka.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import io.paytouch.core.entities.PaginationLinks
import io.paytouch.core.reports.entities._
import io.paytouch.core.reports.entities.enums.ReportInterval
import io.paytouch.core.reports.filters.AdminReportFilters
import io.paytouch.core.reports.views.ReportView
import io.paytouch.core.utils.{ FSpec, MultipleLocationFixtures }
import io.paytouch.core.{ ServiceConfigurations => Config }

abstract class ReportsFSpec[V <: ReportView] extends FSpec {

  def view: V

  def action: String

  val fixtures: ReportsFSpecContext

  abstract class ReportsFSpecContext extends FSpecContext with ReportsDates {

    def buildExpectedResultWhenNoInterval[T](expectedReports: T*): ReportResponse[T] =
      buildExpectedResultWhenNoInterval(None, from, to, expectedReports: _*)

    def buildExpectedResultWhenNoInterval[T](
        fromDate: LocalDateTime,
        toDate: LocalDateTime,
        expectedReports: T*,
      ): ReportResponse[T] =
      buildExpectedResultWhenNoInterval(None, fromDate, toDate, expectedReports: _*)

    def buildExpectedResultWhenNoInterval[T](
        pagination: Option[PaginationLinks],
        fromDate: LocalDateTime,
        toDate: LocalDateTime,
        expectedReports: T*,
      ): ReportResponse[T] = {
      val expectedReportData = ReportData(timeframe = ReportTimeframe(fromDate, toDate), result = expectedReports)
      ReportResponse(meta = ReportMetadata(None, pagination), data = Seq(expectedReportData))
    }

    def buildExpectedResultWithInterval[T](reportData: ReportData[T]*) = {
      val orderedReportData = reportData.sortBy(_.timeframe.start.toString).toList
      ReportResponse(meta = ReportMetadata(ReportInterval.Weekly), data = orderedReportData)
    }

    def assertResponse[T](
        url: String,
        pagination: Option[PaginationLinks],
        fromDate: LocalDateTime,
        toDate: LocalDateTime,
        expectedResults: T*,
      )(implicit
        um: FromResponseUnmarshaller[ReportResponse[T]],
      ) =
      Get(url).addHeader(fixtures.authorizationHeader) ~> fixtures.routes ~> check {
        assertStatusOK()

        val result = responseAs[ReportResponse[T]]
        val expectedResult =
          fixtures.buildExpectedResultWhenNoInterval(pagination, fromDate, toDate, expectedResults: _*)

        result.data.headOption.map(_.result) ==== expectedResult.data.headOption.map(_.result)
        result ==== expectedResult
      }

    def assertResponseWithDefaultDates[T](
        url: String,
        pagination: Option[PaginationLinks],
        expectedResults: T*,
      )(implicit
        um: FromResponseUnmarshaller[ReportResponse[T]],
      ) =
      assertResponse(url, pagination, fixtures.from, fixtures.to, expectedResults: _*)

    def assertFieldResult[T](
        field: String,
        expectedResults: T*,
      )(implicit
        um: FromResponseUnmarshaller[ReportResponse[T]],
      ) =
      s"with field $field" in {
        assertResponseWithDefaultDates(
          s"/v1/reports/${view.endpoint}.$action?${fixtures.defaultParamsNoInterval}&field[]=$field",
          None,
          expectedResults: _*,
        )
      }

    def assertFieldResultWhenNoItems[T](
        field: String,
        expectedResults: T*,
      )(implicit
        um: FromResponseUnmarshaller[ReportResponse[T]],
      ) =
      s"when no items are found" in {
        assertResponse(
          s"/v1/reports/${view.endpoint}.$action?${fixtures.emptyParams}&field[]=$field",
          None,
          fixtures.emptyFrom,
          fixtures.emptyTo,
          expectedResults: _*,
        )
      }

    def assertNoField(): Unit =
      "with no fields it should reject the request" in {
        Get(s"/v1/reports/${view.endpoint}.$action?${fixtures.defaultParamsNoInterval}")
          .addHeader(fixtures.authorizationHeader) ~> fixtures.routes ~> check {
          rejection ==== MissingQueryParamRejection("field[]")
        }
      }
  }
}

trait ReportsDates extends MultipleLocationFixtures {

  val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

  // This is used to prove that this value will be converted to the Rome timezone and match the `from` filter.
  val nowBetweenFromAndFromInRomeTimezone = ZonedDateTime.parse("2015-12-01T20:00:30Z")

  val from = LocalDateTime.parse("2015-12-01T20:15:30")
  val to = LocalDateTime.parse("2015-12-31T20:15:30")

  val start = from
  val end = start.plusDays(7).minusSeconds(1)

  val defaultParamsNoInterval = defaultParams(false)
  val defaultParamsWithInterval = defaultParams(true)

  val emptyFrom = from.plusYears(10)
  val emptyTo = to.plusYears(10)

  val emptyParams = s"from=$emptyFrom&to=$emptyTo"

  private def defaultParams(withInterval: Boolean) = {
    val mandatoryParams = s"from=$from&to=$to"
    if (withInterval) s"$mandatoryParams&with_interval=$withInterval"
    else mandatoryParams
  }

  val adminReportFilters = AdminReportFilters(ids = None, merchantIds = Some(Seq(merchant.id)))
}
