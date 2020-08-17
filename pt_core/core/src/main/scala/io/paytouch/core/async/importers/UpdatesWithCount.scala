package io.paytouch.core.async.importers

import scala.annotation.tailrec

final case class UpdatesWithCount[T](
    updates: Seq[T],
    toAdd: Int,
    toUpdate: Int,
  ) {

  def +(other: UpdatesWithCount[T]): UpdatesWithCount[T] =
    UpdatesWithCount(updates ++ other.updates, toAdd + other.toAdd, toUpdate + other.toUpdate)
}

object UpdatesWithCount {

  def empty[T]: UpdatesWithCount[T] = UpdatesWithCount(Seq.empty[T], 0, 0)

  def sum[T](updates: List[UpdatesWithCount[T]]): UpdatesWithCount[T] = {

    @tailrec
    def recursiveSum(remaining: List[UpdatesWithCount[T]], soFar: UpdatesWithCount[T]): UpdatesWithCount[T] =
      remaining match {
        case List()       => soFar
        case head :: tail => recursiveSum(tail, head + soFar)
      }

    recursiveSum(updates, UpdatesWithCount.empty[T])
  }
}
