package io.paytouch.ordering.json.serializers

import java.time._

import io.paytouch.ordering.utils.Formatters._
import org.json4s.JsonAST.{ JString, JValue }
import org.json4s.{ CustomSerializer, Formats }

case object ZonedDateTimeSerializer
    extends CustomSerializer[ZonedDateTime](formats =>
      (
        ZonedDateTimeSerializerHelper.deserialize(formats),
        {
          case date: ZonedDateTime => JString(ZonedDateTimeFormatter.format(date))
        },
      ),
    )

object ZonedDateTimeSerializerHelper {
  def deserialize(formats: Formats): PartialFunction[JValue, ZonedDateTime] = {
    case JString(x) if ZonedDateTimeFormatter.canParse(x) => ZonedDateTime.parse(x, ZonedDateTimeFormatter)
  }
}

case object LocalDateTimeSerializer
    extends CustomSerializer[LocalDateTime](formats =>
      (
        {
          case JString(x) if LocalDateTimeFormatter.canParse(x) => LocalDateTime.parse(x, LocalDateTimeFormatter)
        },
        {
          case time: LocalDateTime => JString(LocalDateTimeFormatter.format(time))
        },
      ),
    )

case object LocalTimeSerializer
    extends CustomSerializer[LocalTime](formats =>
      (
        {
          case JString(x) if LocalTimeFormatter.canParse(x) => LocalTime.parse(x, LocalTimeFormatter)
        },
        {
          case time: LocalTime => JString(LocalTimeFormatter.format(time))
        },
      ),
    )

object LocalTimeSerializerHelper {

  def deserialize(formats: Formats): PartialFunction[JValue, LocalTime] = {
    case JString(x) if LocalTimeFormatter.canParse(x) => LocalTime.parse(x, LocalTimeFormatter)
  }
}

case object LocalDateSerializer
    extends CustomSerializer[LocalDate](formats =>
      (
        LocalDateSerializerHelper.deserialize(formats),
        {
          case date: LocalDate => JString(LocalDateFormatter.format(date))
        },
      ),
    )

object LocalDateSerializerHelper {

  def deserialize(formats: Formats): PartialFunction[JValue, LocalDate] = {
    case JString(x) if LocalDateFormatter.canParse(x) => LocalDate.parse(x, LocalDateFormatter)
  }
}

case object ZoneIdSerializer
    extends CustomSerializer[ZoneId](formats =>
      (
        {
          case JString(x) if isValidZoneId(x) => ZoneId.of(x)
        },
        {
          case id: ZoneId => JString(id.toString)
        },
      ),
    )
