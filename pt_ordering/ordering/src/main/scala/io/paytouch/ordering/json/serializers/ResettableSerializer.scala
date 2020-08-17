package io.paytouch.ordering.json.serializers

import java.time.{ LocalDate, LocalTime, ZonedDateTime }
import java.util.UUID

import io.paytouch.ordering.entities._
import org.json4s.JsonAST.{ JInt, JNothing, JNull, JString }
import org.json4s.{ CustomSerializer, Extraction, Formats, JValue }

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
          case resettable: R @unchecked =>
            if (resettable.value.contains(None)) JString("")
            else Extraction.decompose(resettable.toOption)
        },
      ),
    )

object ResettableSerializerHelper {

  def resettable[T, R <: Resettable[T]](implicit builder: ResettableHelper[T, R]): PartialFunction[JValue, R] = {
    case JString(x) if x.trim.isEmpty => builder.reset
    case JNull | JNothing             => builder.ignore
  }

  def regularValue[T, R <: Resettable[T]](
      f: PartialFunction[JValue, T],
    )(implicit
      builder: ResettableHelper[T, R],
    ): PartialFunction[JValue, R] = {
    case json if f.isDefinedAt(json) => builder.fromT(f(json))
  }
}

case object ResettableStringSerializer
    extends ResettableSerializer[String, ResettableString](ResettableString)(ScalafmtHelper.deserializeString)

object ScalafmtHelper {
  def deserializeString(formats: Formats): PartialFunction[JValue, String] = {
    case JString(x) => x
  }

  def deserializeInt(formats: Formats): PartialFunction[JValue, Int] = {
    case JInt(x) => x.intValue
  }
}

case object ResettableIntSerializer
    extends ResettableSerializer[Int, ResettableInt](ResettableInt)(ScalafmtHelper.deserializeInt)

case object ResettableBigDecimalSerializer
    extends ResettableSerializer[BigDecimal, ResettableBigDecimal](ResettableBigDecimal)(
      BigDecimalSerializerHelper.deserialize,
    )

case object ResettableUUIDSerializer
    extends ResettableSerializer[UUID, ResettableUUID](ResettableUUID)(UUIDSerializerHelper.deserialize)

case object ResettableLocalDateSerializer
    extends ResettableSerializer[LocalDate, ResettableLocalDate](ResettableLocalDate)(
      LocalDateSerializerHelper.deserialize,
    )

case object ResettableLocalTimeSerializer
    extends ResettableSerializer[LocalTime, ResettableLocalTime](ResettableLocalTime)(
      LocalTimeSerializerHelper.deserialize,
    )

case object ResettableZonedDateTimeSerializer
    extends ResettableSerializer[ZonedDateTime, ResettableZonedDateTime](ResettableZonedDateTime)(
      ZonedDateTimeSerializerHelper.deserialize,
    )
