package io.paytouch.core.json.serializers

import java.time.{ LocalDate, LocalTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.core.entities._
import org.json4s.JsonAST._
import org.json4s.{ CustomSerializer, Extraction, Formats }

/**
  * ResettableSerializer maps the following json:
  * {"key":""} -> reset
  * {"key":null} -> ignore
  * {"key":value} -> value
  * missing from json -> ignore
  */
abstract class ResettableSerializer[T, R <: Resettable[T]: Manifest](
    builder: ResettableHelper[T, R],
  )(
    f: Formats => PartialFunction[JValue, T],
  ) extends CustomSerializer[R](implicit formats =>
      (
        {
          implicit val b: ResettableHelper[T, R] = builder
          import ResettableSerializerHelper._
          resettable orElse regularValue(f(formats))
        },
        {
          case resettable: R =>
            if (resettable.value.contains(None)) JString("")
            else Extraction.decompose(resettable.toOption)
        },
      ),
    )

object ResettableSerializerHelper {
  def resettable[T, R <: Resettable[T]](implicit builder: ResettableHelper[T, R]): PartialFunction[JValue, R] = {
    case JString(x) if x.trim.isEmpty => builder.reset
    case JNull                        => builder.ignore
    case JNothing                     => builder.ignore
  }

  def regularValue[T, R <: Resettable[T]](
      f: PartialFunction[JValue, T],
    )(implicit
      builder: ResettableHelper[T, R],
    ): PartialFunction[JValue, R] = {
    case json if f.isDefinedAt(json) => builder.fromT(f(json))
  }
}

/**
  * ResettableSerializer2 maps the following json:
  * {"key":null} -> reset
  * {"key":value} -> value
  * missing from json -> ignore
  */
abstract class ResettableSerializer2[T, R <: Resettable[T]: Manifest](
    builder: ResettableHelper[T, R],
  )(
    f: Formats => PartialFunction[JValue, T],
  ) extends CustomSerializer[R](implicit formats =>
      (
        {
          implicit val r: ResettableHelper[T, R] = builder
          import ResettableSerializerHelper2._
          resettable orElse regularValue(f(formats))
        },
        {
          case resettable: R =>
            if (resettable.value.contains(None)) JNull
            else Extraction.decompose(resettable.toOption)
        },
      ),
    )

object ResettableSerializerHelper2 {
  def resettable[T, R <: Resettable[T]](implicit builder: ResettableHelper[T, R]): PartialFunction[JValue, R] = {
    case JNull    => builder.reset
    case JNothing => builder.ignore
  }

  def regularValue[T, R <: Resettable[T]](
      f: PartialFunction[JValue, T],
    )(implicit
      builder: ResettableHelper[T, R],
    ): PartialFunction[JValue, R] = {
    case json if f.isDefinedAt(json) => builder.fromT(f(json))
  }
}

object GenericDeserializeHelper {
  def deserialize[T](formats: Formats)(implicit m: Manifest[T]): PartialFunction[JValue, T] = {
    case json =>
      implicit val f: Formats = formats
      json.extract[T]
  }
}

case object ResettableBigDecimalSerializer
    extends ResettableSerializer[BigDecimal, ResettableBigDecimal](ResettableBigDecimal)(
      BigDecimalSerializerHelper.deserialize,
    )

case object ResettableBillingDetailsSerializer
    extends ResettableSerializer[BillingDetails, ResettableBillingDetails](ResettableBillingDetails)(
      GenericDeserializeHelper.deserialize[BillingDetails],
    )

case object ResettableIntSerializer
    extends ResettableSerializer[Int, ResettableInt](ResettableInt)(IntSerializerHelper.deserialize)

case object ResettableLocalDateSerializer
    extends ResettableSerializer[LocalDate, ResettableLocalDate](ResettableLocalDate)(
      LocalDateSerializerHelper.deserialize,
    )

case object ResettableLocalTimeSerializer
    extends ResettableSerializer[LocalTime, ResettableLocalTime](ResettableLocalTime)(
      LocalTimeSerializerHelper.deserialize,
    )

case object ResettableSeatingSerializer
    extends ResettableSerializer2[Seating, ResettableSeating](ResettableSeating)(
      GenericDeserializeHelper.deserialize[Seating],
    )

case object ResettableStringSerializer
    extends ResettableSerializer[String, ResettableString](ResettableString)(
      GenericDeserializeHelper.deserialize[String],
    )

case object ResettableUUIDSerializer
    extends ResettableSerializer[UUID, ResettableUUID](ResettableUUID)(UUIDSerializerHelper.deserialize)

case object ResettableZonedDateTimeSerializer
    extends ResettableSerializer[ZonedDateTime, ResettableZonedDateTime](ResettableZonedDateTime)(
      ZonedDateTimeSerializerHelper.deserialize,
    )
