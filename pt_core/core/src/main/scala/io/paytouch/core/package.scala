package io.paytouch

import java.time._
import java.util.{ Currency, UUID }

import scala.concurrent._

import akka.http.scaladsl.model.headers.RawHeader

import cats._
import cats.data._
import cats.implicits._

import io.paytouch.core.data.model.enums.OrderStatus
import io.paytouch.core.data.model.SlickRecord
import io.paytouch.core.entities.{ Availability, TaxRate }
import io.paytouch.core.entities.Weekdays.Day
import io.paytouch.core.errors.Error
import io.paytouch.utils.{ AppHeaderName, VersionHeaderName }

package object core {
  import io.paytouch.utils.Tagging

  val USD = Currency.getInstance("USD")

  type OrderWorkflow = Seq[OrderStatus]

  type Availabilities = Map[Day, Seq[Availability]]

  type AvailabilitiesPer[A] = Map[A, Availabilities]

  type AvailabilitiesPerItemId = AvailabilitiesPer[UUID]

  type TaxRatesPerLocation = Map[UUID, Seq[TaxRate]]

  type TaxRateIdsPerLocation = Map[UUID, Seq[UUID]]

  type LocationOverridesPer[A, B] = Map[A, Map[UUID, B]]

  type Tag[+U] = Tagging.Tag[U]
  type @@[T, +U] = Tagging.@@[T, U]
  type withTag[T, U] = Tagging.withTag[T, U]
  type Tagged[T, +U] = Tagging.Tagged[T, U]

  import Tagging.{ toAndTagger, toTagger }

  type EntityRef[T] = Map[(String, UUID), T]
  type IdsRef = Map[String, String]

  implicit val ordering: Ordering[ZonedDateTime] = Ordering.fromLessThan(_ isBefore _)

  val ValidationHeaderName = "ErrorCodes"

  def ValidationHeader(errors: NonEmptyList[Error]): RawHeader = {
    val codes = errors.toList.map(_.code).distinct.mkString(", ")
    RawHeader(ValidationHeaderName, codes)
  }

  implicit class NestedOps[F[_]: Functor, G[_]: Functor, A](private val fga: F[G[A]]) {
    // same as
    // fga.nested.as(b).value
    // so let's remove it in the future
    def asNested[B](b: => B): F[G[B]] =
      mapNested(_ => b)

    // same as
    // fga.nested.map(ab).value
    // so let's remove it in the future
    def mapNested[B](ab: A => B): F[G[B]] =
      fga.map(_.map(ab))
  }

  implicit class TransformerOps[F[_]: Monad, G[_]: Traverse, A](private val fga: F[G[A]]) {
    // there is probably sth like this in cats
    // once I find it we will remove this
    def flatMapTraverse[B](afb: A => F[B]): F[G[B]] =
      fga.flatMap(_.traverse(afb))
  }

  implicit class RichAny[A](private val self: A) extends AnyVal {
    def when(f: A => Boolean): Option[A] =
      if (f(self)) Some(self) else None
  }

  implicit class RichMap[V](val mapById: Map[UUID, V]) extends AnyVal {
    def mapKeysToRecords[T <: SlickRecord](objs: Seq[T]): Map[T, V] = mapKeysToObjs(objs)(_.id)

    def mapKeysToObjs[T](objs: Seq[T])(idExtractor: T => UUID): Map[T, V] =
      mapById.flatMap {
        case (k, v) =>
          objs.find(obj => idExtractor(obj) == k).map(obj => (obj, v))
      }

    def merge(other: Map[UUID, V])(f: (V, V) => V, default: V): Map[UUID, V] = {
      val keys = (mapById.keys ++ other.keys).toSet
      keys.map { key =>
        val a = mapById.getOrElse(key, default)
        val b = other.getOrElse(key, default)
        key -> f(a, b)
      }
    }.toMap
  }

  implicit class SequenceOfOptionIds[A](val xs: Iterable[Iterable[A]]) extends AnyVal {
    // cartesian product of an arbitrary number of traversables: http://stackoverflow.com/a/8569263
    def combine: Seq[Seq[A]] =
      if (xs.isEmpty)
        Seq.empty
      else
        xs.foldLeft(Seq(Seq.empty[A])) { (acc, current) =>
          (for {
            a <- acc.view
            b <- current
          } yield a :+ b).toSeq
        }
  }

  implicit class RichLocalTime(time: LocalTime) {
    def nonAfter(other: LocalTime): Boolean = !time.isAfter(other)
    def nonBefore(other: LocalTime): Boolean = !time.isBefore(other)

    def isBetween(start: LocalTime, end: LocalTime): Boolean =
      if (start isAfter end) !isBetween(end, start)
      else time.nonBefore(start) && time.nonAfter(end)

    def isAbout(other: LocalTime): Boolean = {
      val toleranceInHours = 2
      time.isBetween(other.minusHours(toleranceInHours), other.plusHours(toleranceInHours))
    }
  }

  implicit class RichZoneDateTime(datetime: ZonedDateTime) {
    def toLocationTimezone(zoneId: ZoneId) =
      datetime.withZoneSameInstant(zoneId)

    def toLocationTimezoneWithFallback(zoneId: Option[ZoneId]) =
      toLocationTimezone(zoneId.getOrElse(ZoneId.of("UTC")))

    def asZulu = datetime.withZoneSameLocal(ZoneId.of("Z"))
  }
}
