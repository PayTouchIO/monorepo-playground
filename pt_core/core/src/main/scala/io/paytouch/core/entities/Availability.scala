package io.paytouch.core.entities

import java.time.LocalTime
import io.paytouch.core.Availabilities

final case class Availability(start: LocalTime, end: LocalTime)

object Availability {
  val TwentyFourHours =
    Availability(start = LocalTime.MIN, end = LocalTime.MAX)
}

object Weekdays extends Enumeration {
  type Day = Value
  val Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday = Value
}

object Availabilities {
  import Weekdays._

  val TwentyFourSeven: Availabilities =
    Map(
      Sunday -> Seq(Availability.TwentyFourHours),
      Monday -> Seq(Availability.TwentyFourHours),
      Tuesday -> Seq(Availability.TwentyFourHours),
      Wednesday -> Seq(Availability.TwentyFourHours),
      Thursday -> Seq(Availability.TwentyFourHours),
      Friday -> Seq(Availability.TwentyFourHours),
      Saturday -> Seq(Availability.TwentyFourHours),
    )
}
