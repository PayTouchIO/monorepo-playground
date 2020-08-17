package io.paytouch.ordering.graphql.datatypes

import java.time.ZoneId

import sangria.ast
import sangria.schema.ScalarType
import sangria.validation.{ ValueCoercionViolation, Violation }

import scala.util.{ Failure, Success, Try }

trait ZoneIdDataType {

  private case class ZoneIdDataViolation(msg: String) extends ValueCoercionViolation(s"ZoneId expected: $msg")

  private def parseZoneId(s: String): Either[Violation, ZoneId] =
    Try(ZoneId.of(s)) match {
      case Success(zone) => Right(zone)
      case Failure(ex)   => Left(ZoneIdDataViolation(ex.getMessage))
    }

  lazy val ZoneIdType = ScalarType[ZoneId](
    "ZoneId",
    coerceOutput = (zoneId, _) => zoneId.toString,
    coerceUserInput = {
      case s: String => parseZoneId(s)
      case _         => Left(ZoneIdDataViolation("No ZoneId provided"))
    },
    coerceInput = {
      case s: ast.StringValue => parseZoneId(s.value)
      case _                  => Left(ZoneIdDataViolation("Unknown error"))
    },
  )
}
