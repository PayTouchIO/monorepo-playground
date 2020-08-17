package io.paytouch.implicits

/**
  * It is safer to use these, because whereas
  *
  * b1 // forgotten && or ||
  * b2
  *
  * compiles, the following does not:
  *
  * Boolean.and(
  *   b1 // forgotten ,
  *   b2
  * )
  *
  * Note 1: Scala's varargs * annotation does compile with by name parameters =>.
  * Note 2: Scalafmt formats Boolean.and and Boolean.or better than vertical && and ||.
  */
trait BooleanOpsModule {
  final implicit class PaytouchBooleanOps(private val self: Boolean.type) {
    @inline def and(b1: Boolean, b2: => Boolean): Boolean =
      b1 && b2

    @inline def and(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
      ): Boolean =
      b1 && b2 && b3

    @inline def and(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
      ): Boolean =
      b1 && b2 && b3 && b4

    @inline def and(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
      ): Boolean =
      b1 && b2 && b3 && b4 && b5

    @inline def and(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
        b6: => Boolean,
      ): Boolean =
      b1 && b2 && b3 && b4 && b5 && b6

    @inline def and(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
        b6: => Boolean,
        b7: => Boolean,
      ): Boolean =
      b1 && b2 && b3 && b4 && b5 && b6 && b7

    @inline def and(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
        b6: => Boolean,
        b7: => Boolean,
        b8: => Boolean,
      ): Boolean =
      b1 && b2 && b3 && b4 && b5 && b6 && b7 && b8

    @inline def and(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
        b6: => Boolean,
        b7: => Boolean,
        b8: => Boolean,
        b9: => Boolean,
      ): Boolean =
      b1 && b2 && b3 && b4 && b5 && b6 && b7 && b8 && b9

    @inline def and(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
        b6: => Boolean,
        b7: => Boolean,
        b8: => Boolean,
        b9: => Boolean,
        b10: => Boolean,
      ): Boolean =
      b1 && b2 && b3 && b4 && b5 && b6 && b7 && b8 && b9 && b10

    @inline def or(b1: Boolean, b2: => Boolean): Boolean =
      b1 || b2

    @inline def or(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
      ): Boolean =
      b1 || b2 || b3

    @inline def or(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
      ): Boolean =
      b1 || b2 || b3 || b4

    @inline def or(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
      ): Boolean =
      b1 || b2 || b3 || b4 || b5

    @inline def or(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
        b6: => Boolean,
      ): Boolean =
      b1 || b2 || b3 || b4 || b5 || b6

    @inline def or(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
        b6: => Boolean,
        b7: => Boolean,
      ): Boolean =
      b1 || b2 || b3 || b4 || b5 || b6 || b7

    @inline def or(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
        b6: => Boolean,
        b7: => Boolean,
        b8: => Boolean,
      ): Boolean =
      b1 || b2 || b3 || b4 || b5 || b6 || b7 || b8

    @inline def or(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
        b6: => Boolean,
        b7: => Boolean,
        b8: => Boolean,
        b9: => Boolean,
      ): Boolean =
      b1 || b2 || b3 || b4 || b5 || b6 || b7 || b8 || b9

    @inline def or(
        b1: Boolean,
        b2: => Boolean,
        b3: => Boolean,
        b4: => Boolean,
        b5: => Boolean,
        b6: => Boolean,
        b7: => Boolean,
        b8: => Boolean,
        b9: => Boolean,
        b10: => Boolean,
      ): Boolean =
      b1 || b2 || b3 || b4 || b5 || b6 || b7 || b8 || b9 || b10
  }
}
