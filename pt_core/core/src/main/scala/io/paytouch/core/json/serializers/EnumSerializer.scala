package io.paytouch.core.json.serializers

import enumeratum._
import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

class EnumSerializer[A <: EnumEntry: Manifest](enum: Enum[A])
    extends CustomSerializer[A](formats =>
      (
        {
          case JString(x) if enum.withNameOption(x).isDefined => enum.withName(x)
        },
        {
          case enumEntry: A => JString(enumEntry.entryName)
        },
      ),
    )
