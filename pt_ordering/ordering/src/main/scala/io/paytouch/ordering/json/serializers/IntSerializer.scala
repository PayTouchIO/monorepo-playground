package io.paytouch.ordering.json.serializers

import io.paytouch.ordering.utils.Formatters._
import org.json4s.CustomSerializer
import org.json4s.JsonAST.{ JInt, JString }

case object IntSerializer
    extends CustomSerializer[Int](formats =>
      (
        {
          case JString(x) if isValidInt(x) => x.toInt
          case JInt(i)                     => i.toInt
        },
        {
          case i: Int => JInt(i)
        },
      ),
    )
