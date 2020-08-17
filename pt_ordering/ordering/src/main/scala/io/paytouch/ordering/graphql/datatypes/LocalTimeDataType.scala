package io.paytouch.ordering.graphql.datatypes

import java.time.LocalTime

import io.paytouch.ordering.utils.Formatters.LocalTimeFormatter
import sangria.ast
import sangria.schema.ScalarType
import sangria.validation.{ ValueCoercionViolation, Violation }

import scala.util.{ Failure, Success, Try }

trait LocalTimeDataType {
  private case class LocalTimeDataViolation(msg: String) extends ValueCoercionViolation(s"LocalTime expected: $msg")

  private def parseLocalTime(s: String): Either[Violation, LocalTime] =
    Try(LocalTime.parse(s, LocalTimeFormatter)) match {
      case Success(zone) => Right(zone)
      case Failure(ex)   => Left(LocalTimeDataViolation(ex.getMessage))
    }

  lazy val LocalTimeType = ScalarType[LocalTime](
    "LocalTime",
    coerceOutput = (time, _) => LocalTimeFormatter.format(time),
    coerceUserInput = {
      case s: String => parseLocalTime(s)
      case _         => Left(LocalTimeDataViolation("No LocalTime provided"))
    },
    coerceInput = {
      case s: ast.StringValue => parseLocalTime(s.value)
      case _                  => Left(LocalTimeDataViolation("Unknown error"))
    },
  )
}
