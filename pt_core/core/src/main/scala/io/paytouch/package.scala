package io

import cats.data._

package object paytouch {
  type Nec[+A] = NonEmptyChain[A]
  val Nec = NonEmptyChain

  type Nel[+A] = NonEmptyList[A]
  val Nel = NonEmptyList

  type Nem[K, +A] = NonEmptyMap[K, A]
  val Nem = NonEmptyMap

  type Nes[A] = NonEmptySet[A]
  val Nes = NonEmptySet
}
