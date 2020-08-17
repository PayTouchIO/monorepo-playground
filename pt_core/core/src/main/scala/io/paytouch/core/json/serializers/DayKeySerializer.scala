package io.paytouch.core.json.serializers

import io.paytouch.core.entities.Weekdays
import io.paytouch.core.entities.Weekdays.Day
import io.paytouch.core.utils.Formatters._
import org.json4s.CustomKeySerializer
import io.paytouch.core.utils.RichString._

case object DayKeySerializer
    extends CustomKeySerializer[Day](formats =>
      (
        { case s: String if isValidDay(s) => Weekdays.withName(s.pascalize) },
        {
          case d: Day => d.toString.capitalize
        },
      ),
    )
