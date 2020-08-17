package io.paytouch.core.entities

import java.time.{ LocalDate, LocalTime, ZonedDateTime }
import java.util.UUID

trait Resettable[T] {
  def value: Option[Option[T]]
  val toOption = value.flatten

  def getOrElse(optT: Option[T]): Option[T] = value.getOrElse(optT)
  def isDefined = toOption.isDefined
  def map[B] = value.map[B](_)
}

trait ResettableHelper[T, R <: Resettable[T]] {
  def apply(optOptT: Option[Option[T]]): R
  val ignore: R = apply(None)
  val reset: R = apply(Some(None))

  // conversion to resettable
  implicit def fromOptOptT(optOptT: Option[Option[T]]): R = apply(optOptT)
  implicit def fromOptT(optT: Option[T]): R = fromOptOptT(optT.map(Some(_)))
  implicit def fromT(t: T): R = fromOptT(Some(t))
  implicit def fromNone(none: None.type): R = fromOptT(none)

  // conversion from resettable
  implicit def toOptOptT(resettable: R): Option[Option[T]] = resettable.value
  implicit def toOptT(resettable: R): Option[T] = toOptOptT(resettable).flatten
}

final case class ResettableBigDecimal(value: Option[Option[BigDecimal]]) extends Resettable[BigDecimal]
object ResettableBigDecimal extends ResettableHelper[BigDecimal, ResettableBigDecimal]

final case class ResettableBillingDetails(value: Option[Option[LegalDetails]]) extends Resettable[LegalDetails]
object ResettableBillingDetails extends ResettableHelper[LegalDetails, ResettableBillingDetails]

final case class ResettableInt(value: Option[Option[Int]]) extends Resettable[Int]
object ResettableInt extends ResettableHelper[Int, ResettableInt]

final case class ResettableLocalDate(value: Option[Option[LocalDate]]) extends Resettable[LocalDate]
object ResettableLocalDate extends ResettableHelper[LocalDate, ResettableLocalDate]

final case class ResettableLocalTime(value: Option[Option[LocalTime]]) extends Resettable[LocalTime]
object ResettableLocalTime extends ResettableHelper[LocalTime, ResettableLocalTime]

final case class ResettableSeating(value: Option[Option[Seating]]) extends Resettable[Seating]
object ResettableSeating extends ResettableHelper[Seating, ResettableSeating]

final case class ResettableString(value: Option[Option[String]]) extends Resettable[String]
object ResettableString extends ResettableHelper[String, ResettableString]

final case class ResettableUUID(value: Option[Option[UUID]]) extends Resettable[UUID]
object ResettableUUID extends ResettableHelper[UUID, ResettableUUID]

final case class ResettableZonedDateTime(value: Option[Option[ZonedDateTime]]) extends Resettable[ZonedDateTime]
object ResettableZonedDateTime extends ResettableHelper[ZonedDateTime, ResettableZonedDateTime]
