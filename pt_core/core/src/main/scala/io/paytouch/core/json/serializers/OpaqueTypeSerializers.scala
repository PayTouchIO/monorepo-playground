package io.paytouch.core.json.serializers

import java.util.UUID

import org.json4s.CustomSerializer
import org.json4s.JsonAST._

import io.paytouch._

object OpaqueTypeSerializers {
  val all = List(
    CountrySerializer.CodeSerializer,
    CountrySerializer.NameSerializer,
    GiftCardPassSerializer.IdSerializer,
    GiftCardPassSerializer.OnlineCodeSerializer.HyphenatedSerializer,
    GiftCardPassSerializer.OnlineCodeSerializer.HyphenlessSerializer,
    GiftCardPassSerializer.OnlineCodeSerializer.RawSerializer,
    MerchantIdSerializer,
    OrderIdSerializer,
    StateSerializer.CodeSerializer,
    StateSerializer.NameSerializer,
  )
}

case object CountrySerializer {
  case object CodeSerializer extends OpaqueString(CountryCode)
  case object NameSerializer extends OpaqueString(CountryName)
}

case object GiftCardPassSerializer {
  case object IdSerializer extends OpaqueString(GiftCardPass.Id)

  case object OnlineCodeSerializer {
    case object HyphenatedSerializer extends OpaqueString(GiftCardPass.OnlineCode.Hyphenated)
    case object HyphenlessSerializer extends OpaqueString(GiftCardPass.OnlineCode)
    case object RawSerializer extends OpaqueString(GiftCardPass.OnlineCode.Raw)
  }
}

case object MerchantIdSerializer extends OpaqueString(MerchantId)

case object OrderIdSerializer extends OpaqueString(OrderId)

case object StateSerializer {
  case object CodeSerializer extends OpaqueString(StateCode)
  case object NameSerializer extends OpaqueString(StateName)
}

// There should be a way to define only one serializer for all OpaqueTypes but
// I neither know enough about Json4s nor do I want to spend any more time on this.
class OpaqueString[O <: Opaque[String]: Manifest](f: String => O)
    extends CustomSerializer[O](formats =>
      (
        {
          case JString(value) => f(value)
        },
        {
          case o: O => JString(o.value)
        },
      ),
    )

class OpaqueBoolean[O <: Opaque[Boolean]: Manifest](f: Boolean => O)
    extends CustomSerializer[O](formats =>
      (
        {
          case JBool(value) => f(value)
        },
        {
          case o: O => JBool(o.value)
        },
      ),
    )
