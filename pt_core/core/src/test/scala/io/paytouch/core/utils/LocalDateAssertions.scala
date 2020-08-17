package io.paytouch.core.utils
import java.time.LocalTime

import org.specs2.matcher.{ Expectable, Matcher }

trait LocalDateAssertions {

  /**
    *  Updates for localtime with value 23:59:59.999 get saved in the db as 24:00:00.0 and deserialized
    *  back to 23:59:59.999999999
    *
    *  This helper approximates the match to the 3rd decimal position
    * @param date
    * @return
    */
  def beApproxTheSame(date: LocalTime) =
    new Matcher[LocalTime] {
      val thresholdInNanoSeconds = Math.pow(10, 6) // approximates to the 3rd decimal position
      def apply[D <: LocalTime](e: Expectable[D]) = {
        val nanoDifference = e.value.toNanoOfDay - date.toNanoOfDay
        result(
          nanoDifference < thresholdInNanoSeconds,
          "LocalTimes are close enough",
          s"LocalTime are far apart (${e.value} - $date = $nanoDifference nanoseconds)",
          e,
        )
      }
    }
}
