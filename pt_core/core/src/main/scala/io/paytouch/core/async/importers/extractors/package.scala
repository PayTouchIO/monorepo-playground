package io.paytouch.core.async.importers

import cats.data.ValidatedNel

package object extractors {

  implicit class RichSeq[T](val seq: Seq[T]) extends AnyVal {

    def distinctBy[K](f: T => K): Seq[T] = seq.groupBy(f).map { case (_, v) => v.head }.toSeq
  }

  implicit class RichOption[T](val op: Option[T]) extends AnyVal {

    def matches(seq: Seq[T]): Boolean =
      op match {
        case Some(t) => seq.contains(t)
        case None    => seq.isEmpty
      }
  }

  implicit class RichValidatedUpdatesWithCount[A, B](val validatedNel: ValidatedNel[A, UpdatesWithCount[B]])
      extends AnyVal {

    def entities: Seq[B] = validatedNel.map(_.updates).getOrElse(Seq.empty)
  }

  implicit class RichValidated[A, B](val validatedNel: ValidatedNel[A, Seq[B]]) extends AnyVal {

    def entities: Seq[B] = validatedNel.getOrElse(Seq.empty)
  }
}
