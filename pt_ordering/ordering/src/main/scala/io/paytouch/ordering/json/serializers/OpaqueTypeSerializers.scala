package io.paytouch.ordering.json.serializers

import java.util.UUID

import org.json4s.CustomSerializer
import org.json4s.JsonAST._

import io.paytouch._
import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities.stripe.Livemode

object OpaqueTypeSerializers {
  val all = List(
    CountrySerializer.CodeSerializer,
    CountrySerializer.NameSerializer,
    GiftCardPassSerializer.Id,
    GiftCardPassSerializer.OnlineCodeSerializer.RawSerializer,
    OrderIdSerializer,
    StateSerializer.CodeSerializer,
    StateSerializer.NameSerializer,
    StripeSerializer.LivemodeSerializer,
  )
}

case object CountrySerializer {
  case object CodeSerializer extends OpaqueString(Country.Code)
  case object NameSerializer extends OpaqueString(Country.Name)
}

case object GiftCardPassSerializer {
  case object Id extends OpaqueString(io.paytouch.GiftCardPass.Id)

  case object OnlineCodeSerializer {
    case object RawSerializer extends OpaqueString(io.paytouch.GiftCardPass.OnlineCode.Raw)
  }
}

case object OrderIdSerializer extends OpaqueString(OrderId)

case object StateSerializer {
  case object CodeSerializer extends OpaqueString(State.Code)
  case object NameSerializer extends OpaqueString(State.Name)
}

case object StripeSerializer {
  case object LivemodeSerializer extends OpaqueBoolean(Livemode)
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
