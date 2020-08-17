package io.paytouch.core.validators

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.core._
import io.paytouch.core.data._
import io.paytouch.core.data.daos._
import io.paytouch.core.entities.ModifierSetUpdate
import io.paytouch.core.errors._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.features.DefaultValidator

class ModifierSetValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[model.ModifierSetRecord] {
  type Record = model.ModifierSetRecord
  type Dao = ModifierSetDao

  protected val dao = daos.modifierSetDao
  val validationErrorF = InvalidModifierSetIds(_)
  val accessErrorF = NonAccessibleModifierSetIds(_)

  def validateModifierOptionLimits(update: ModifierSetUpdate): EitherNel[Error, Option[ModifierOptionCount]] =
    (update.minimumOptionCount, update.maximumOptionCount) match {
      case (Some(min), Some(max)) => ModifierOptionCount(min.pipe(Minimum), max.map(Maximum)).bimap(Nel.one, _.some)
      case (Some(min), None)      => ModifierOptionCount.toInfinity(Minimum(min)).bimap(Nel.one, _.some)
      case (None, Some(_))        => ModifierMinimumOptionCountNotSpecified.leftNel
      case (None, None)           => calculate(update.singleChoice, update.force)
    }

  private def calculate(
      singleChoice: Option[Option[Boolean]],
      force: Option[Option[Boolean]],
    ): EitherNel[Error, Option[ModifierOptionCount]] =
    (singleChoice.nested, force.nested)
      .mapN {
        case (false, false) => ModifierOptionCount.unsafeFromZero
        case (false, true)  => ModifierOptionCount.unsafeFromOne
        case (true, false)  => ModifierOptionCount.unsafeApply(Minimum(0), Maximum(1).some)
        case (true, true)   => ModifierOptionCount.unsafeApply(Minimum(1), Maximum(1).some)
      }
      .value
      .flatten
      .map(_.some.rightNel)
      .getOrElse(NeitherModifierCountsNorLegacyBooleanFlagsAreSepcified.leftNel)

  def validateMaximumSingleOptionCount(
      optionCount: Option[ModifierOptionCount],
      maximumSingleOptionCount: Option[Option[Int]],
    ): EitherNel[Error, Option[Int]] =
    (optionCount.map(_.maximum), maximumSingleOptionCount).tupled.fold(none[Int].rightNel[Error]) {
      case (Some(maximumOptionCount), Some(maximumSingleOptionCount)) =>
        if (maximumOptionCount.value >= maximumSingleOptionCount)
          maximumSingleOptionCount.some.rightNel
        else
          MaximumOptionCountMustNotBeSmallerThanMaximumSingleOptionCount(
            maximumOptionCount = maximumOptionCount.value,
            maximumSingleOptionCount = maximumSingleOptionCount,
          ).leftNel

      case (None, Some(maximumSingleOptionCount)) =>
        maximumSingleOptionCount.some.rightNel

      case _ =>
        None.rightNel
    }
}
