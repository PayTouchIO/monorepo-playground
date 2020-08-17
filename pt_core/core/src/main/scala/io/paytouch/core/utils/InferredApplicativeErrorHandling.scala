package io.paytouch.core.utils

import cats._
import cats.data._
import cats.implicits._

trait InferredApplicativeMultipleErrorsHandling[F[_, _], E]
    extends InferredApplicativeErrorHandling[F[NonEmptyList[E], *], E] {
  final type Errors = NonEmptyList[E]
  final type MultipleErrorsOr[A] = ErrorOr[A]

  final implicit class MultipleBadOps(private val e: E) {
    def bad[A](implicit F: ApplicativeError[MultipleErrorsOr, _ >: Errors]): MultipleErrorsOr[A] =
      F.raiseError(e.pure[NonEmptyList])
  }

  final implicit class GoodOrMultipleBadOps[A](private val option: Option[A]) {
    final def goodOrBad(
        error: => E,
      )(implicit
        F: ApplicativeError[MultipleErrorsOr, _ >: Errors],
      ): MultipleErrorsOr[A] =
      option.fold[MultipleErrorsOr[A]](error.bad)(_.good)
  }
}

trait InferredApplicativeErrorHandling[F[_], E] {
  final type Error = E
  final type ErrorOr[A] = F[A]

  final implicit class GoodOps[A](private val a: A) {
    def good(implicit F: Applicative[F]): F[A] =
      F.pure(a)
  }

  final implicit class BadOps(private val e: Error) {
    def bad[A](implicit F: ApplicativeError[F, _ >: Error]): F[A] =
      F.raiseError(e)
  }

  final implicit class GoodOrBadOps[A](private val option: Option[A]) {
    final def goodOrBad(error: => Error)(implicit F: ApplicativeError[F, _ >: Error]): F[A] =
      option.fold[F[A]](error.bad)(_.good)
  }
}
