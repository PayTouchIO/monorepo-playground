package io.paytouch.core.reports.entities.enums

import java.time.LocalDateTime

import io.paytouch.core.errors.ExceededIntervalsInTimeRange
import io.paytouch.core.utils.ValidatedHelpers
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class ReporIntervalSpec extends Specification with ValidatedHelpers {
  import ReportInterval._

  abstract class ReportIntervalSpecContext extends Scope {
    val date = LocalDateTime.of(2017, 1, 23, 12, 34, 59)
  }

  "ReportInterval" should {
    "detect the expected interval" in {

      "when no interval is requested" in new ReportIntervalSpecContext {
        val interval = validateAndDetectInterval(date, date.plusDays(2), withInterval = false, forceInterval = None)
        interval.success ==== NoInterval
      }

      "when force interval is defined" should {
        "if requested value generates too many intervals" in new ReportIntervalSpecContext {
          val interval =
            validateAndDetectInterval(date, date.plusDays(30), withInterval = false, forceInterval = Some(Hourly))
          interval.failures ==== Seq(ExceededIntervalsInTimeRange(720, 400))
        }

        "if requested value generates a decent amount of intervals" in new ReportIntervalSpecContext {
          val interval =
            validateAndDetectInterval(date, date.plusDays(1), withInterval = false, forceInterval = Some(Daily))
          interval.success ==== Daily
        }
      }

      "when interval is requested" should {

        "if diff of date is up to 2 days" in new ReportIntervalSpecContext {
          val interval = validateAndDetectInterval(date, date.plusDays(2), withInterval = true, forceInterval = None)
          interval.success ==== Hourly
        }

        "if diff of date is 3 days" in new ReportIntervalSpecContext {
          val interval = validateAndDetectInterval(date, date.plusDays(3), withInterval = true, forceInterval = None)
          interval.success ==== Daily
        }

        "if diff of date is up to 2 weeks" in new ReportIntervalSpecContext {
          val interval = validateAndDetectInterval(date, date.plusWeeks(2), withInterval = true, forceInterval = None)
          interval.success ==== Daily
        }

        "if diff of date is up to 2 months" in new ReportIntervalSpecContext {
          val interval = validateAndDetectInterval(date, date.plusMonths(2), withInterval = true, forceInterval = None)
          interval.success ==== Weekly
        }

        "if diff of date is up to 2 years" in new ReportIntervalSpecContext {
          val interval = validateAndDetectInterval(date, date.plusYears(2), withInterval = true, forceInterval = None)
          interval.success ==== Monthly
        }

        "if diff of date is more than 2 years" in new ReportIntervalSpecContext {
          val interval = validateAndDetectInterval(date, date.plusYears(10), withInterval = true, forceInterval = None)
          interval.success ==== Yearly
        }
      }
    }
  }
}
