package io.paytouch.core.utils

import java.time.ZonedDateTime
import scala.concurrent.duration.FiniteDuration
import TimeImplicits._

import org.specs2.matcher.{ Expectable, Matcher }

trait ZonedDateTimeAssertions {

  def beWithin(duration: FiniteDuration, expect: ZonedDateTime) =
    new Matcher[ZonedDateTime] {
      def apply[D <: ZonedDateTime](e: Expectable[D]) = {
        val rangeStart = expect - duration
        val rangeEnd = expect + duration

        val actual = e.value
        val within = rangeStart.isBefore(actual) && rangeEnd.isAfter(actual)

        result(
          within,
          "ZonedDateTimes are close enough",
          s"ZonedDateTime are far apart ($actual not in range $rangeStart to $rangeEnd)",
          e,
        )
      }
    }
}
