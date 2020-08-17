package io.paytouch.core.utils

import cats.data._

import io.paytouch._

import io.paytouch.core.errors

import Multiple._

sealed abstract class ResultType extends SerializableProduct
object ResultType {
  case object Created extends ResultType
  case object Updated extends ResultType
}

object UpsertionResult {
  def apply[A](result: Result[A]): ErrorsOr[Result[A]] =
    Validated.Valid(result)

  def invalid[A](nel: NonEmptyList[errors.Error]): ErrorsOr[Result[A]] =
    Validated.Invalid(nel)
}

object FindResult {
  type FindResult[T] = (Seq[T], Int)
}
