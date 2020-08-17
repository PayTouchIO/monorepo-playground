package io.paytouch.ordering.json.serializers

import io.paytouch.ordering.utils.Formatters._
import org.json4s.JsonAST._
import org.json4s.{ CustomSerializer, Formats }

case object BigDecimalSerializer
    extends CustomSerializer[BigDecimal](implicit formats =>
      (
        BigDecimalSerializerHelper.deserialize(formats),
        {
          case bd: BigDecimal => JString(bd.toString)
        },
      ),
    )

object BigDecimalSerializerHelper {

  def deserialize(formats: Formats): PartialFunction[JValue, BigDecimal] = {
    case JString(x) if isValidBigDecimal(x)           => BigDecimal(x)
    case JInt(n) if isValidBigDecimal(n.toString)     => BigDecimal(n.toString)
    case JDecimal(n) if isValidBigDecimal(n.toString) => BigDecimal(n.toString)
    case JDouble(n) if isValidBigDecimal(n.toString)  => BigDecimal(n.toString)
    case jsonObj: JObject =>
      implicit val f: Formats = formats
      (jsonObj \ "amount").extract[BigDecimal]
  }
}
