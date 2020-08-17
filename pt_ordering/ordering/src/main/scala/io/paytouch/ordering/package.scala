package io.paytouch

import java.util.UUID

import scala.concurrent._

import cats.data._

import io.paytouch.ordering.data.model.SlickRecord
import io.paytouch.ordering.utils.ResultType
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

package object ordering {
  val ValidationHeaderName = "ErrorCodes"
  val AppHeaderName = "Paytouch-App-Name"
  val VersionHeaderName = "Paytouch-App-Version"
  val LocationHeaderName = "Location"

  type Result[T] = (ResultType, T)
  type UpsertionResult[T] = ValidatedData[Result[T]]
  type FindResult[T] = (Seq[T], Int)

  type Tag[+U] = { type Tag <: U }
  type @@[T, +U] = T with Tag[U]
  type withTag[T, U] = @@[T, U]
  type Tagged[T, +U] = T with Tag[U]

  final implicit class Tagger[T](val t: T) extends AnyVal {
    def taggedWith[U]: T @@ U = t.asInstanceOf[T @@ U]
  }

  final implicit class AndTagger[T, U](val t: T @@ U) extends AnyVal {
    def andTaggedWith[V]: T @@ (U with V) = t.asInstanceOf[T @@ (U with V)]
  }

  final implicit class RichMap[V](val mapById: Map[UUID, V]) {
    def mapKeysToRecords[T <: SlickRecord](objs: Seq[T]): Map[T, V] =
      mapKeysToObjs(objs)(_.id)

    def mapKeysToObjs[T](objs: Seq[T])(idExtractor: T => UUID): Map[T, V] =
      mapById.flatMap {
        case (k, v) =>
          objs
            .find(obj => idExtractor(obj) == k)
            .map(_ -> v)
      }
  }

  final implicit class FutureValidation[E, T](val fv: Future[ValidatedNel[E, T]]) {
    def mapValid[S](f: T => S)(implicit ec: ExecutionContext): Future[ValidatedNel[E, S]] =
      fv.map {
        case Validated.Valid(a)       => Validated.Valid(f(a))
        case i @ Validated.Invalid(_) => i
      }

    def flatMapValid[S](f: T => Future[S])(implicit ec: ExecutionContext): Future[ValidatedNel[E, S]] =
      fv.flatMap {
        case Validated.Valid(a)       => f(a).map(Validated.Valid(_))
        case i @ Validated.Invalid(_) => Future.successful(i)
      }

    def onValid[S](f: T => Future[ValidatedNel[E, S]])(implicit ec: ExecutionContext): Future[ValidatedNel[E, S]] =
      fv.flatMap {
        case Validated.Valid(a)       => f(a)
        case i @ Validated.Invalid(_) => Future.successful(i)
      }
  }
}
