package io.paytouch.core.json.serializers

import enumeratum.Enum
import io.paytouch.core.utils.{ EnumEntrySnake, PaytouchSpec }
import org.json4s.JsonAST.JArray
import org.json4s._
import org.json4s.native.Serialization

class EnumSerializerSpec extends PaytouchSpec {

  object CustomEnums {
    sealed trait FooBarType extends EnumEntrySnake

    case object FooBarType extends Enum[FooBarType] {

      case object One extends FooBarType
      case object PascalCased extends FooBarType

      val values = findValues
    }
  }

  import CustomEnums._

  implicit val formats: Formats = json4sFormats + new EnumSerializer(FooBarType)

  "An EnumSerializer" should {
    "serialize to pascal case enum values" in {
      val types = JArray("one, pascal_cased".split(",").map(s => JString(s.trim)).toList)

      types.extract[List[FooBarType]] ==== List(FooBarType.One, FooBarType.PascalCased)
    }

    "deserialize to underscored strings" in {
      Serialization.write(List(FooBarType.One, FooBarType.PascalCased)) ==== """["one","pascal_cased"]"""
    }

  }
}
