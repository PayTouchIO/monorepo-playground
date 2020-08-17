package io.paytouch.core.json.serializers

import java.util.UUID

import io.paytouch.core.utils.RegexUtils._
import org.json4s.CustomKeySerializer

case object UUIDKeySerializer
    extends CustomKeySerializer[UUID](formats =>
      ({ case x: String if isValidUUID(x) => UUID.fromString(x) }, { case u: UUID => u.toString }),
    )
