package io.paytouch.ordering.json.serializers

import java.util.UUID

import io.paytouch.ordering.utils.Formatters._
import org.json4s.CustomKeySerializer

case object UUIDKeySerializer
    extends CustomKeySerializer[UUID](formats =>
      ({ case x: String if isValidUUID(x) => UUID.fromString(x) }, { case u: UUID => u.toString }),
    )
