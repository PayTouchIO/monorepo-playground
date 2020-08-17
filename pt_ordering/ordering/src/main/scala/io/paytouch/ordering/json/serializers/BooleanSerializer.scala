package io.paytouch.ordering.json.serializers

import io.paytouch.ordering.entities.{ BooleanFalse, BooleanTrue, BooleanWithDefault }
import org.json4s.CustomSerializer
import org.json4s.JsonAST.{ JBool, JInt, JNothing, JNull }

case object BooleanSerializer
    extends CustomSerializer[Boolean](formats =>
      (
        {
          case JBool(b) => b
          case JInt(i)  => i == BigInt(1)
        },
        {
          case b: Boolean => JBool(b)
        },
      ),
    )

case object BooleanFalseSerializer extends BooleanWithDefaultSerializer(BooleanFalse.apply, false)
case object BooleanTrueSerializer extends BooleanWithDefaultSerializer(BooleanTrue.apply, true)

class BooleanWithDefaultSerializer[T <: BooleanWithDefault: Manifest](f: Boolean => T, default: Boolean)
    extends CustomSerializer[T](formats =>
      (
        {
          case JBool(b)         => f(b)
          case JInt(i)          => f(i == BigInt(1))
          case JNull | JNothing => f(default)
        },
        {
          case b: BooleanWithDefault => JBool(b)
        },
      ),
    )
