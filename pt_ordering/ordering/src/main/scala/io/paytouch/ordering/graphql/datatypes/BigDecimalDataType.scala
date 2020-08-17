package io.paytouch.ordering.graphql.datatypes

import sangria.ast
import sangria.schema.ScalarType
import sangria.validation.{ ValueCoercionViolation, Violation }

import scala.util.{ Failure, Success, Try }

trait BigDecimalDataType {

  private case class BigDecimalDataViolation(msg: String) extends ValueCoercionViolation(s"BigDecimal expected: $msg")

  private def parseBigDecimal(s: String): Either[Violation, BigDecimal] =
    Try(BigDecimal(s)) match {
      case Success(d)  => Right(d)
      case Failure(ex) => Left(BigDecimalDataViolation(ex.getMessage))
    }

  lazy val BigDecimalType = ScalarType[BigDecimal](
    "BigDecimal",
    coerceOutput = (d, _) => d.toString,
    coerceUserInput = {
      case s: String      => parseBigDecimal(s)
      case bd: BigDecimal => Right(bd)
      case n: Int         => Right(n)
      case _              => Left(BigDecimalDataViolation("No BigDecimal provided"))
    },
    coerceInput = {
      case s: ast.StringValue      => parseBigDecimal(s.value)
      case bd: ast.BigDecimalValue => Right(bd.value)
      case n: ast.IntValue         => Right(n.value)
      case _                       => Left(BigDecimalDataViolation("Unknown error"))
    },
  )
}
