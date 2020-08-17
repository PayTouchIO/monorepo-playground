package io.paytouch.implicits

trait AnyOpsModule {
  final implicit class PayTouchAnyOps[A](private val a: A) {
    def pipe[B](f: A => B): B = f(a)
    def tap[U](f: A => U): A = { f(a); a }
    def tapAs[U](u: => U): A = { u; a }

    /**
      * "w" stands for "widened".
      *
      * Int is a more "narrow" type compared to a BigDecimal.
      * BigDecimal is a "wider" type compared to Int.
      *
      * Even though this relationship is expressed with implicits in Scala
      * it can be thought of as Int extends BigDecimal or Int <: BigDecimal
      * because every function that wants a BigDecimal can receive an Int as argument.
      *
      * Scala never applies 2 implicit conversions at once so the 1.some example
      * below does not compile because it would require a conversion from Int to BigDecimal
      * and then another one from Int to some Wrapper which has the .some method.
      *
      * I don't know why the signature of .some in cats does not look like the signature of .somew.
      * I'm actually considering submitting a PR to cats.
      *
      * val big: Option[BigDecimal] = 1.some  // does not compile
      * val big: Option[BigDecimal] = 1.somew // compiles
      */
    def somew[B](implicit f: A => B): Option[B] = Some(f(a))
  }
}
