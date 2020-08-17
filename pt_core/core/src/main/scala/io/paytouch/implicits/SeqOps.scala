package io.paytouch.implicits

import cats._
import cats.implicits._

trait SeqOpsModule {
  final implicit class PaytouchSeqOps[F[+_], A](
      private val self: F[Seq[A]],
    )(implicit
      ev1: Functor[F],
      ev2: Semigroup[F[List[A]]],
    ) {
    final def combineWithOne(one: F[A]): F[Seq[A]] =
      self.map(_.toList) combine one.map(_.pure[List])
  }
}
