package io.paytouch.core.json.serializers

import org.json4s.CustomSerializer
import org.json4s.JsonAST.{ JBool, JInt, JString }

case object BooleanSerializer
    extends CustomSerializer[Boolean](formats =>
      (
        {
          case JBool(b)         => b
          case JInt(i)          => i == BigInt(1)
          case JString("true")  => true
          case JString("false") => false
        },
        {
          case b: Boolean => JBool(b)
        },
      ),
    )
