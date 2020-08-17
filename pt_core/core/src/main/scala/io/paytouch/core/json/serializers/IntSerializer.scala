package io.paytouch.core.json.serializers

import io.paytouch.core.utils.Formatters._
import org.json4s.JsonAST._
import org.json4s.{ CustomSerializer, Formats }

case object IntSerializer
    extends CustomSerializer[Int](formats =>
      (
        IntSerializerHelper.deserialize(formats),
        {
          case i: Int => JInt(i)
        },
      ),
    )

object IntSerializerHelper {
  def deserialize(format: Formats): PartialFunction[JValue, Int] = {
    case JString(x) if isValidInt(x) => x.toInt
    case JInt(i)                     => i.toInt
  }
}
