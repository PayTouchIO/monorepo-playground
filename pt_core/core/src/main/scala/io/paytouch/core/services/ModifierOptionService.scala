package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import io.paytouch.core.conversions.ModifierOptionConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model._
import io.paytouch.core.entities.{ UserContext, ModifierOption => ModifierOptionEntity }
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.utils.Multiple

import io.paytouch.core.validators.ModifierOptionValidator

class ModifierOptionService(implicit val ec: ExecutionContext, val daos: Daos) extends ModifierOptionConversions {
  protected val dao = daos.modifierOptionDao
  protected val validator = new ModifierOptionValidator

  def convertToModifierOptionUpdates(
      modifierSetId: UUID,
      options: Option[Seq[ModifierOptionEntity]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Seq[ModifierOptionUpdate]]]] =
    options match {
      case Some(opts) =>
        validator.validateByIdsWithModifierSetId(opts.map(_.id), modifierSetId).mapNested { _ =>
          val updates = opts.map(toModelOptionUpdate(modifierSetId, _))
          Some(updates)
        }
      case None => Future.successful(Multiple.empty)
    }

  def findByModifierSets(
      modifierSets: Seq[ModifierSetRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[ModifierOptionEntity]]] =
    dao
      .findByModifierSetIds(modifierSets.map(_.id))
      .map(_.groupBy(_.modifierSetId))
      .map(convert(modifierSets))

  private def convert(
      modifierSets: Seq[ModifierSetRecord],
    )(
      groups: Map[UUID, Seq[ModifierOptionRecord]],
    )(implicit
      user: UserContext,
    ): Map[UUID, Seq[ModifierOptionEntity]] =
    for {
      (modifierSetId, groupedOptionRecords) <- groups
      modifierSetRecord <- modifierSets.find(_.id === modifierSetId)
    } yield modifierSetId -> fromRecordsToEntities(modifierSetRecord)(groupedOptionRecords)
}
