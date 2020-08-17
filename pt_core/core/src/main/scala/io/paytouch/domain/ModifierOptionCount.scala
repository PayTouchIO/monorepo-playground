package io.paytouch

import cats.data._
import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.errors.ModifierOptionErrors

final case class ModifierOptionCount(minimum: Minimum, maximum: Option[Maximum]) {
  def force: Boolean =
    minimum > Minimum(0)

  def singleChoice: Boolean =
    maximum.exists(_ === Maximum(1))
}

object ModifierOptionCount
    extends Function2[Minimum, Option[Maximum], Either[ModifierOptionErrors, ModifierOptionCount]] {
  def fromZero: Either[ModifierOptionErrors, ModifierOptionCount] =
    toInfinity(Minimum(0))

  def fromOne: Either[ModifierOptionErrors, ModifierOptionCount] =
    toInfinity(Minimum(1))

  def toInfinity(minimum: Minimum): Either[ModifierOptionErrors, ModifierOptionCount] =
    apply(minimum, maximum = None)

  override def apply(minimum: Minimum, maximum: Option[Maximum]): Either[ModifierOptionErrors, ModifierOptionCount] =
    (checkMinimum(minimum), checkMaximum(maximum), checkMinimumVsMaximum(minimum, maximum))
      .parTupled
      .as(new ModifierOptionCount(minimum, maximum))
      .leftMap(ModifierOptionErrors)

  private def checkMinimum(minimum: Minimum): EitherNel[String, Any] =
    if (minimum >= Minimum(0))
      ().rightNel
    else
      s"Expected minimum_option_count to be >= 0, but was ${minimum.value}.".leftNel

  private def checkMaximum(maximum: Option[Maximum]): EitherNel[String, Any] =
    maximum.fold(().rightNel[String]) { max =>
      if (max >= Maximum(0))
        ().rightNel
      else
        s"Expected maximum_option_count to be >= 0, but was ${max.value}.".leftNel
    }

  private def checkMinimumVsMaximum(minimum: Minimum, maximum: Option[Maximum]): EitherNel[String, Any] =
    maximum.fold(().rightNel[String]) { max =>
      if (minimum.value <= max.value)
        ().rightNel
      else
        s"Expected ${minimum.value} to be <= ${max.value}.".leftNel
    }

  def unsafeFromZero: ModifierOptionCount =
    fromZero.getOrElse(sys.error("invariant violation"))

  def unsafeFromOne: ModifierOptionCount =
    fromOne.getOrElse(sys.error("invariant violation"))

  def unsafeInfinite(minimum: Minimum): ModifierOptionCount =
    toInfinity(minimum).getOrElse(sys.error("invariant violation"))

  def unsafeApply(minimum: Minimum, maximum: Option[Maximum]): ModifierOptionCount =
    apply(minimum, maximum).getOrElse(sys.error("invariant violation"))
}
