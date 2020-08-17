package io.paytouch.core.services

import java.util.UUID

import cats.implicits._

import io.paytouch.core.conversions.ModifierSetProductConversions
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.ModifierSetProductRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.{ MainArticleValidator, ModifierSetValidator }
import io.paytouch.core.entities.EntityOrdering

import scala.concurrent._

class ModifierSetProductService(implicit val ec: ExecutionContext, val daos: Daos)
    extends ModifierSetProductConversions {

  protected val dao = daos.modifierSetProductDao

  val articleValidator = new MainArticleValidator
  val modifierSetValidator = new ModifierSetValidator

  def findByProductIds(productIds: Seq[UUID]): Future[Seq[ModifierSetProductRecord]] =
    dao.findByProductIds(productIds)

  def findPerProductIds(productIds: Seq[UUID]): Future[Map[UUID, Seq[ModifierSetProductRecord]]] =
    dao.findByProductIds(productIds).map(_.groupBy(_.productId))

  def countByModifierSetIds(modifierSetIds: Seq[UUID]): Future[Map[UUID, Int]] =
    dao.countByModifierSetIds(modifierSetIds)

  // Legacy update method
  def associateModifierSetIdsToProduct(
      productId: UUID,
      modifierSetIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    modifierSetValidator.accessByIds(modifierSetIds).flatMapTraverse { _ =>
      val updates = toModifierSetProducts(Seq(productId), modifierSetIds)
      dao.bulkUpsertAndDeleteTheRestByProductIds(updates, Seq(productId)).void
    }

  def associateModifierSetsToProduct(
      productId: UUID,
      modifierSets: Seq[EntityOrdering],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] = {
    val modifierSetIds = modifierSets.map(eo => eo.id)
    modifierSetValidator.accessByIds(modifierSetIds).flatMapTraverse { _ =>
      val updates = toModifierSetProductsUpdates(productId, modifierSets)
      dao.bulkUpsertAndDeleteTheRestByProductIds(updates, Seq(productId)).void
    }
  }

  def associateProductsToModifierSet(
      modifierSetId: UUID,
      productIds: Seq[UUID],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Unit]] =
    articleValidator.accessByIds(productIds).flatMapTraverse { _ =>
      val updates = toModifierSetProducts(productIds = productIds, modifierSetIds = Seq(modifierSetId))
      dao.bulkUpsertAndDeleteTheRestByModifierSetIds(updates, Seq(modifierSetId)).void
    }
}
