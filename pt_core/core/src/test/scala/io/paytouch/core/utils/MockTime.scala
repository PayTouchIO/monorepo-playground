package io.paytouch.core.utils

import java.time._

object MockTime extends TimeProvider {

  val now = ZonedDateTime.of(2016, 1, 4, 12, 54, 43, 0, ZoneOffset.UTC)

  val thisInstant = now.toInstant
}
