package io.paytouch.core.utils

import java.time.ZonedDateTime
import scala.concurrent.duration.FiniteDuration

object TimeImplicits {
  implicit class TimeImplicitsZonedDateTime(val t: ZonedDateTime) extends AnyVal {
    // Example:
    //
    // import scala.concurrent.duration._
    // import io.paytouch.core.utils.TimeImplicits._
    //
    // > UtcTime.now + 45.minutes
    // java.time.ZonedDateTime = 2020-01-22T13:13:27.275Z
    def +(d: FiniteDuration): ZonedDateTime = t.plusNanos(d.toNanos)
    def -(d: FiniteDuration): ZonedDateTime = t.minusNanos(d.toNanos)
  }
}
