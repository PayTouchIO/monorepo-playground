package io.paytouch

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.data.model.{ SlickRecord, UserRecord }
import io.paytouch.core.utils.ResultType

import scala.concurrent._
import scala.util.Random

package object seeds extends LazyLogging {

  implicit class RichString(val s: String) extends AnyVal {
    def toUUID = UUID.nameUUIDFromBytes(s.getBytes)
  }

  implicit class RichSeq[T](val seq: Seq[T]) extends AnyVal {
    def cartesian[X](xs: Seq[X]): Seq[(T, X)] = for { s <- seq; x <- xs } yield (s, x)

    def random: T = seq(Random.nextInt(seq.size))

    def randomSample: Seq[T] = {
      val randomSize = if (seq.isEmpty) 0 else Random.nextInt(seq.size)
      random(randomSize)
    }

    def randomAtLeast(n: Int): Seq[T] = {
      val base = random(n)
      val extras = (seq diff base).randomSample
      base ++ extras
    }

    def random(n: Int): Seq[T] = shuffle.take(n)

    def shuffle: Seq[T] = Random.shuffle(seq)

    def splitInRandomGroups(n: Int): Seq[Seq[T]] =
      if (n <= 0) Seq(shuffle)
      else {
        val step: Int = seq.size / n
        shuffle.sliding(step, step).toSeq
      }
  }

  implicit class RichFutureBulkUpsertResult[T](val f: Future[Seq[(ResultType, T)]]) extends AnyVal {
    import scala.reflect._

    def extractRecords(
        implicit
        ct: ClassTag[T],
        user: UserRecord,
        ec: ExecutionContext,
      ): Future[Seq[T]] =
      f.map {
        case results =>
          val records = results.map { case (_, record) => record }
          val className = ct.runtimeClass.getSimpleName
          logger.info(s"[${user.email}] ${records.size} $className have been inserted")
          records
      }

    def extractRecords(email: String)(implicit ct: ClassTag[T], ec: ExecutionContext): Future[Seq[T]] =
      f.map {
        case results =>
          val records = results.map { case (_, record) => record }
          val className = ct.runtimeClass.getSimpleName
          logger.info(s"[$email] ${records.size} $className have been inserted")
          records
      }
  }

  implicit class RichFutureUpsertResult[T <: SlickRecord](val f: Future[(ResultType, T)]) extends AnyVal {
    import scala.reflect._

    def extractRecord(email: String)(implicit ct: ClassTag[T], ec: ExecutionContext): Future[T] =
      f.map {
        case (_, record) =>
          val className = ct.runtimeClass.getSimpleName
          logger.info(s"[$email] $className has been inserted")
          record
      }
  }
}
