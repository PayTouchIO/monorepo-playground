package io.paytouch.implicits

import java.time.ZonedDateTime

trait ZonedDateTimeModule {
  implicit val orderingForZonedDateTime: Ordering[ZonedDateTime] =
    Ordering.fromLessThan(_ isBefore _)
}
