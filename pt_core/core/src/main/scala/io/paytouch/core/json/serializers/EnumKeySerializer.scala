package io.paytouch.core.json.serializers

import enumeratum._
import org.json4s.CustomKeySerializer
import io.paytouch.core.utils.RichString._

class EnumKeySerializer[A <: EnumEntry: Manifest](enum: Enum[A])
    extends CustomKeySerializer[A](formats =>
      (
        EnumKeySerializer.deserialize(enum),
        {
          case enumEntry: A => enumEntry.entryName
        },
      ),
    )

object EnumKeySerializer {
  def deserialize[A <: EnumEntry](enum: Enum[A]): PartialFunction[String, A] = {
    case x if enum.withNameOption(x).isDefined            => enum.withName(x)
    case x if enum.withNameOption(x.pascalize).isDefined  => enum.withName(x.pascalize)
    case x if enum.withNameOption(x.underscore).isDefined => enum.withName(x.underscore)
  }
}
