package io.paytouch.ordering.graphql.datatypes

import java.util.UUID

import sangria.ast
import sangria.schema.ScalarType
import sangria.validation.{ ValueCoercionViolation, Violation }

import scala.util.{ Failure, Success, Try }

trait UUIDDataType {
  private case class UUIDDataViolation(msg: String) extends ValueCoercionViolation(s"UUID expected: $msg")

  private def parseUUID(s: String): Either[Violation, UUID] =
    Try(UUID.fromString(s)) match {
      case Success(uuid) => Right(uuid)
      case Failure(ex)   => Left(UUIDDataViolation(ex.getMessage))
    }

  lazy val UUIDType = ScalarType[UUID](
    "UUID",
    coerceOutput = (uuid, _) => uuid.toString,
    coerceUserInput = {
      case s: String => parseUUID(s)
      case _         => Left(UUIDDataViolation("No UUID provided"))
    },
    coerceInput = {
      case s: ast.StringValue => parseUUID(s.value)
      case _                  => Left(UUIDDataViolation("Unknown error"))
    },
  )
}
