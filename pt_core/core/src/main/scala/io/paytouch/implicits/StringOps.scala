package io.paytouch.implicits

import cats.implicits._

trait StringOpsModule {
  final implicit class PaytouchStringOps(private val self: String) {
    def hyphenatedAfterEvery(position: Int): String =
      self
        .zipWithIndexOne
        .map {
          case (char, index) =>
            if (index % position == 0 && index < self.size)
              char.toString + '-'
            else
              char.toString
        }
        .mkString

    def hyphenless: String =
      self.replace("-", "")

    def zipWithIndexOne: IndexedSeq[(Char, Int)] =
      self.toIndexedSeq.zipWithIndex.map(_.map(_ + 1))
    // self.toIndexedSeq.zipWithIndexOne
  }
}
