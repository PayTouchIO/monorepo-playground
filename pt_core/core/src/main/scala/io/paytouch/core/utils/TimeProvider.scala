package io.paytouch.core.utils

import java.time._

trait TimeProvider {
  def thisInstant: Instant

  def now: ZonedDateTime
}

object UtcTime extends TimeProvider {
  def thisInstant: Instant = Instant.now(Clock.systemUTC)

  def now: ZonedDateTime = ZonedDateTime.now(Clock.systemUTC)

  def ofInstant(instant: Instant): ZonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"))

  def ofLocalDateTime(date: LocalDateTime): ZonedDateTime = date.atZone(ZoneId.of("UTC"))
}
