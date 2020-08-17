package io.paytouch.core.utils

import cats.data._

trait ValidatedHelpers {
  final implicit class RichValidatedNel[E, T](private val vd: ValidatedNel[E, T]) {
    def failures: Seq[E] =
      vd match {
        case Validated.Invalid(i) =>
          i.toList

        case _ =>
          throw new NoSuchElementException(s"Wanted failure, but found success. $vd")
      }

    def success: T =
      vd match {
        case Validated.Valid(t) =>
          t

        case _ =>
          throw new NoSuchElementException(s"Wanted success, but found failure. $vd")
      }
  }
}
