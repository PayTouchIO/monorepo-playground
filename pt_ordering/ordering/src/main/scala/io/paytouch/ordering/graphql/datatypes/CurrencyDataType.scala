package io.paytouch.ordering.graphql.datatypes

import java.util.Currency

import sangria.ast
import sangria.schema.ScalarType
import sangria.validation.{ ValueCoercionViolation, Violation }

import scala.util.{ Failure, Success, Try }

trait CurrencyDataType {

  private case class CurrencyDataViolation(msg: String) extends ValueCoercionViolation(s"Currency expected: $msg")

  private def parseCurrency(s: String): Either[Violation, Currency] =
    Try(Currency.getInstance(s.toUpperCase)) match {
      case Success(curr) => Right(curr)
      case Failure(ex)   => Left(CurrencyDataViolation(ex.getMessage))
    }

  lazy val CurrencyType = ScalarType[Currency](
    "Currency",
    coerceOutput = (curr, _) => curr.getCurrencyCode.toUpperCase,
    coerceUserInput = {
      case s: String => parseCurrency(s)
      case _         => Left(CurrencyDataViolation("No Currency provided"))
    },
    coerceInput = {
      case s: ast.StringValue => parseCurrency(s.value)
      case _                  => Left(CurrencyDataViolation("Unknown error"))
    },
  )
}
