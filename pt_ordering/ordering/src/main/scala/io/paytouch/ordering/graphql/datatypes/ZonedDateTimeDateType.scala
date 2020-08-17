package io.paytouch.ordering.graphql.datatypes

import java.time.ZonedDateTime

import sangria.ast
import sangria.schema.ScalarType
import sangria.validation.{ ValueCoercionViolation, Violation }

import scala.util.{ Failure, Success, Try }

import io.paytouch.ordering.utils.Formatters.ZonedDateTimeFormatter

trait ZonedDateTimeDataType {
  private case class ZonedDateTimeDataViolation(msg: String)
      extends ValueCoercionViolation(s"ZonedDateTime expected: $msg")
  lazy val ZonedDateTimeType = ScalarType[ZonedDateTime](
    "ZonedDateTime",
    coerceOutput = (d, _) => ZonedDateTimeFormatter.format(d),
    coerceUserInput = {
      case _ => Left(ZonedDateTimeDataViolation("Not implemented"))
    },
    coerceInput = {
      case _ => Left(ZonedDateTimeDataViolation("Not implemented"))
    },
  )
}
