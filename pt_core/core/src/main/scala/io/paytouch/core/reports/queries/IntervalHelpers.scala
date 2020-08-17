package io.paytouch.core.reports.queries
import io.paytouch.core.data.driver.CustomPlainImplicits._
import io.paytouch.core.reports.filters.ReportFilters

trait IntervalHelpers {
  protected lazy val intervalSeries = "intervals"
  protected lazy val intervalStartTime = "start_time"
  protected lazy val intervalEndTime = "end_time"
  protected lazy val intervalStartTimeSelector = s"$intervalSeries.$intervalStartTime"
  protected lazy val intervalEndTimeSelector = s"$intervalSeries.$intervalEndTime"
  protected lazy val intervalSelectors = Seq(intervalStartTimeSelector, intervalEndTimeSelector)

  protected def intervalsView(filters: ReportFilters): String = {
    val cast = "timestamp"
    val step = filters.interval.step(filters.from, filters.to)
    val zonedFrom = localDateTimeAsString(filters.from)
    val zonedTo = localDateTimeAsString(filters.to)
    s"""SELECT n AS $intervalStartTime, n::$cast + '$step' - '1 seconds'::interval as $intervalEndTime
       | FROM generate_series($zonedFrom::$cast, $zonedTo::$cast, '$step') n
       """.stripMargin
  }
}
