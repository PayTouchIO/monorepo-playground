package io.paytouch.core

import cats.data._

import io.paytouch.core.errors.Error

package object utils {
  type DependentErrorHandling[E <: Error] = InferredApplicativeMultipleErrorsHandling[Either, E]
  type IndependentErrorHandling[E <: Error] = InferredApplicativeMultipleErrorsHandling[Validated, E]

  type Result[+A] = (ResultType, A)
}
