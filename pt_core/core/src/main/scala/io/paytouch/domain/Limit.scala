package io.paytouch

import cats._
import cats.data._
import cats.syntax.all._

// I accidentally built this and in the end decided not to use it yet, so for now it is not used.
sealed abstract class Limit[From[_], To[_], A] extends SerializableProduct {
  def from: From[A]
  def to: To[A]

  def isBetween(range: Range)(implicit ev: A =:= Int): Boolean
}

object Limit {
  final case class Exclusive[A] private[Exclusive] (from: A, to: A) extends Limit[Id, Id, A] {
    override val productPrefix: String =
      "Limit.Exclusive"

    override def isBetween(range: Range)(implicit ev: A =:= Int): Boolean =
      Range(from, to) == range
  }

  object Exclusive {
    def make[A](from: A, to: A)(implicit A: Ordering[A]): EitherNel[String, Exclusive[A]] = {
      import A._

      if (from < to)
        unsafeApply(from, to).rightNel
      else
        s"Expected $from to be < $to.".leftNel
    }

    def unsafeApply[A](from: A, to: A): Exclusive[A] =
      new Exclusive(from, to)

    final case class WithOptionalLowerBound[A] private[WithOptionalLowerBound] (from: Option[A], to: A)
        extends Limit[Option, Id, A] {
      override val productPrefix: String =
        "Limit.Exclusive.WithOptionalLowerBound"

      override def isBetween(range: Range)(implicit ev: A =:= Int): Boolean =
        from.forall(Range(_, to) == range)
    }

    object WithOptionalLowerBound {
      def to[A](to: A): WithOptionalLowerBound[A] =
        new WithOptionalLowerBound(from = None, to)

      def make[A](from: Option[A], to: A)(implicit A: Ordering[A]): EitherNel[String, WithOptionalLowerBound[A]] = {
        import A._

        from.fold(unsafeApply(from, to).rightNel[String]) { f =>
          if (f < to)
            unsafeApply(from, to).rightNel
          else
            s"Expected $f to be < $to.".leftNel
        }
      }

      def unsafeApply[A](from: Option[A], to: A): WithOptionalLowerBound[A] =
        new WithOptionalLowerBound(from, to)
    }

    final case class WithOptionalUpperBound[A] private[WithOptionalUpperBound] (from: A, to: Option[A])
        extends Limit[Id, Option, A] {
      override val productPrefix: String =
        "Limit.Exclusive.WithOptionalUpperBound"

      override def isBetween(range: Range)(implicit ev: A =:= Int): Boolean =
        to.forall(Range(from, _) == range)
    }

    object WithOptionalUpperBound {
      def from[A](from: A): WithOptionalUpperBound[A] =
        new WithOptionalUpperBound(from, to = None)

      def make[A](from: A, to: Option[A])(implicit A: Ordering[A]): EitherNel[String, WithOptionalUpperBound[A]] = {
        import A._

        to.fold(unsafeApply(from, to).rightNel[String]) { t =>
          if (from < t)
            unsafeApply(from, to).rightNel
          else
            s"Expected $from to be < $t.".leftNel
        }
      }

      def unsafeApply[A](from: A, to: Option[A]): WithOptionalUpperBound[A] =
        new WithOptionalUpperBound(from, to)
    }
  }

  final case class Inclusive[A] private[Inclusive] (from: A, to: A) extends Limit[Id, Id, A] {
    override val productPrefix: String =
      "Limit.Inclusive"

    override def isBetween(range: Range)(implicit ev: A =:= Int): Boolean =
      Range.inclusive(from, to) == range
  }

  object Inclusive {
    def make[A](from: A, to: A)(implicit A: Ordering[A]): EitherNel[String, Inclusive[A]] = {
      import A._

      if (from <= to)
        unsafeApply(from, to).rightNel
      else
        s"Expected $from to be <= $to.".leftNel
    }

    def unsafeApply[A](from: A, to: A): Inclusive[A] =
      new Inclusive(from, to)

    final case class WithOptionalLowerBound[A] private[WithOptionalLowerBound] (from: Option[A], to: A)
        extends Limit[Option, Id, A] {
      override val productPrefix: String =
        "Limit.Inclusive.WithOptionalLowerBound"

      override def isBetween(range: Range)(implicit ev: A =:= Int): Boolean =
        from.forall(Range.inclusive(_, to) == range)
    }

    object WithOptionalLowerBound {
      def to[A](to: A): WithOptionalLowerBound[A] =
        new WithOptionalLowerBound(from = None, to)

      def make[A](from: Option[A], to: A)(implicit A: Ordering[A]): EitherNel[String, WithOptionalLowerBound[A]] = {
        import A._

        from.fold(unsafeApply(from, to).rightNel[String]) { f =>
          if (f <= to)
            unsafeApply(from, to).rightNel
          else
            s"Expected $f to be <= $to.".leftNel
        }
      }

      def unsafeApply[A](from: Option[A], to: A): WithOptionalLowerBound[A] =
        new WithOptionalLowerBound(from, to)
    }

    final case class WithOptionalUpperBound[A] private[WithOptionalUpperBound] (from: A, to: Option[A])
        extends Limit[Id, Option, A] {
      override val productPrefix: String =
        "Limit.Inclusive.WithOptionalUpperBound"

      override def isBetween(range: Range)(implicit ev: A =:= Int): Boolean =
        to.forall(Range.inclusive(from, _) == range)
    }

    object WithOptionalUpperBound {
      def from[A](from: A): WithOptionalUpperBound[A] =
        new WithOptionalUpperBound(from, to = None)

      def make[A](from: A, to: Option[A])(implicit A: Ordering[A]): EitherNel[String, WithOptionalUpperBound[A]] = {
        import A._

        to.fold(unsafeApply(from, to).rightNel[String]) { t =>
          if (from <= t)
            unsafeApply(from, to).rightNel
          else
            s"Expected $from to be <= $t.".leftNel
        }
      }

      def unsafeApply[A](from: A, to: Option[A]): WithOptionalUpperBound[A] =
        new WithOptionalUpperBound(from, to)
    }
  }
}
