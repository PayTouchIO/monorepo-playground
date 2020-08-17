package io.paytouch.ordering.json.serializers

import enumeratum._
import io.paytouch.ordering.utils.StringHelper._
import org.json4s.CustomKeySerializer

class EnumKeySerializer[A <: EnumEntry: Manifest](enum: Enum[A])
    extends CustomKeySerializer[A](formats =>
      (
        {
          case x: String if enum.withNameOption(x).isDefined            => enum.withName(x)
          case x: String if enum.withNameOption(x.pascalize).isDefined  => enum.withName(x.pascalize)
          case x: String if enum.withNameOption(x.underscore).isDefined => enum.withName(x.underscore)
        },
        {
          case enumEntry: A => enumEntry.entryName
        },
      ),
    )
