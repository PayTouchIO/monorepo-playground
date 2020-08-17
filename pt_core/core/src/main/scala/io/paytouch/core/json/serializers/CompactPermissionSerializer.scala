package io.paytouch.core.json.serializers

import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

import io.paytouch.core.data.model.Permission

case object CompactPermissionSerializer
    extends CustomSerializer[Permission](formats =>
      (
        {
          case JString(x) => Permission.fromRepresentation(x)
        },
        {
          case p: Permission => JString(p.representation)
        },
      ),
    )
