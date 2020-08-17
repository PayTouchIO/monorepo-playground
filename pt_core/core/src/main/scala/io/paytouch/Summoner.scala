package io.paytouch

object the extends SummonerA

trait SummonerA {
  def apply[A](implicit a: A): A =
    implicitly[A]
}

trait SummonerB[B[_]] {
  final def apply[A: B]: B[A] =
    implicitly[B[A]]
}

trait SummonerC[C[_[_]]] {
  final def apply[B[_]: C]: C[B] =
    implicitly[C[B]]
}
