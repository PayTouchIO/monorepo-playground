package io.paytouch.ordering.json.serializers

import java.util.UUID

import io.paytouch.ordering.utils.Formatters._
import org.json4s.JsonAST.{ JString, JValue }
import org.json4s.{ CustomSerializer, Formats }

case object UUIDSerializer
    extends CustomSerializer[UUID](formats =>
      (
        UUIDSerializerHelper.deserialize(formats),
        {
          case id: UUID => JString(id.toString)
        },
      ),
    )

object UUIDSerializerHelper {

  def deserialize(formats: Formats): PartialFunction[JValue, UUID] = {
    case JString(x) if isValidUUID(x) => UUID.fromString(x)
  }
}
