package io.paytouch.utils

object Tagging {
  type Tag[+U] = { type Tag <: U }
  type @@[T, +U] = T with Tag[U]
  type withTag[T, U] = @@[T, U]
  type Tagged[T, +U] = T with Tag[U]

  class Tagger[T](val t: T) extends AnyVal {
    def taggedWith[U]: T @@ U = t.asInstanceOf[T @@ U]
  }

  class AndTagger[T, U](val t: T @@ U) extends AnyVal {
    def andTaggedWith[V]: T @@ (U with V) = t.asInstanceOf[T @@ (U with V)]
  }

  implicit def toTagger[T](t: T) = new Tagger[T](t)
  implicit def toAndTagger[T, U](t: T @@ U) = new AndTagger[T, U](t)
}
