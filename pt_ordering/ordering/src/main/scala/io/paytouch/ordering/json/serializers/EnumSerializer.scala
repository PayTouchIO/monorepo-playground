package io.paytouch.ordering.json.serializers

import enumeratum._
import org.json4s.CustomSerializer
import org.json4s.JsonAST.JString

class EnumSerializer[A <: EnumEntry: Manifest](enum: Enum[A])
    extends CustomSerializer[A](formats =>
      (
        {
          case JString(x) if enum.withNameOption(x).isDefined             => enum.withName(x)
          case JString(x) if enum.withNameOption(x.toLowerCase).isDefined => enum.withName(x.toLowerCase)
        },
        {
          case enumEntry: A => JString(enumEntry.entryName)
        },
      ),
    )
