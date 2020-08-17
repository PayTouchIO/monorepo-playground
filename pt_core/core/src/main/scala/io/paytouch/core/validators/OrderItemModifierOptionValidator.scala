package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ ModifierOptionRecord, ModifierSetRecord }
import io.paytouch.core.data.model.enums.ModifierSetType
import io.paytouch.core.entities._
import io.paytouch.core.errors.{ EmptyModifierOptionIds, InvalidModifierOptionIds }

import io.paytouch.core.utils.Multiple._

import io.paytouch.core.utils.{ Multiple, PaytouchLogger }

import scala.concurrent._

class OrderItemModifierOptionValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) {

  val modifierOptionValidator = new ModifierOptionValidator
  val modifierSetValidator = new ModifierSetValidator

  def validateUpsertions(
      modifierOptionsPerOrderId: Map[UUID, Seq[OrderItemModifierOptionUpsertion]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[OrderItemModifierOptionUpsertion]]] = {
    val modifierOptionIds = modifierOptionsPerOrderId.values.flatten.flatMap(_.modifierOptionId).toSeq
    for {
      modifierOptions <- modifierOptionValidator.filterValidByIds(modifierOptionIds)
      modifierSetIds = modifierOptions.map(_.modifierSetId)
      modifierSets <- modifierSetValidator.filterValidByIds(modifierSetIds)
    } yield Multiple.sequence(
      modifierOptionsPerOrderId.map {
        case (orderItemId, upsertions) =>
          Multiple.combineSeq(
            upsertions.map { upsertion =>
              val validModifierOptionId = recoverModifierOptionId(modifierOptions, upsertion.modifierOptionId)
              val validModifierSetName =
                recoverModifierSetName(upsertion.modifierOptionId, modifierOptions, modifierSets)
              Multiple.combine(validModifierOptionId, validModifierSetName) { case _ => upsertion }
            },
          )
      },
    )
  }

  def recoverUpsertions(
      modifierOptionsPerOrderId: Map[UUID, Seq[OrderItemModifierOptionUpsertion]],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[RecoveredOrderItemModifierOptionUpsertion]]] = {
    val modifierOptionIds = modifierOptionsPerOrderId.values.flatten.flatMap(_.modifierOptionId).toSeq
    for {
      modifierOptions <- modifierOptionValidator.filterValidByIds(modifierOptionIds)
      modifierSetIds = modifierOptions.map(_.modifierSetId)
      modifierSets <- modifierSetValidator.filterValidByIds(modifierSetIds)
    } yield modifierOptionsPerOrderId.map {
      case (orderItemId, upsertions) =>
        val recoveredUpsertions = upsertions.map { upsertion =>
          val recoveredModifierOptionId =
            logger.loggedSoftRecover(recoverModifierOptionId(modifierOptions, upsertion.modifierOptionId))(
              "Modifier option not found, assuming it has been deleted",
            )
          val recoveredModifierSetName =
            logger.loggedRecover(recoverModifierSetName(recoveredModifierOptionId, modifierOptions, modifierSets))(
              "While finding modifier set name for modifier option upsertion",
              upsertion,
            )
          RecoveredOrderItemModifierOptionUpsertion(
            modifierOptionId = recoveredModifierOptionId,
            name = upsertion.name,
            modifierSetName = recoveredModifierSetName,
            `type` = upsertion.`type`,
            price = MonetaryAmount(upsertion.price),
            quantity = upsertion.quantity,
          )
        }
        orderItemId -> recoveredUpsertions
    }
  }

  private def recoverModifierSetName(
      optionId: Option[UUID],
      modifierOptions: Seq[ModifierOptionRecord],
      modifierSets: Seq[ModifierSetRecord],
    ): ErrorsOr[Option[String]] = {
    val modifierSetName = for {
      modifierOption <- modifierOptions.find(mo => optionId.contains(mo.id))
      modifierSet <- modifierSets.find(modifierSet => modifierSet.id == modifierOption.modifierSetId)
    } yield modifierSet.name
    modifierSetName match {
      case Some(name) => Multiple.successOpt(name)
      case _          => Multiple.failure(InvalidModifierOptionIds(optionId.toSeq))
    }
  }

  private def recoverModifierOptionId(
      modifierOptions: Seq[ModifierOptionRecord],
      modifierOptionId: Option[UUID],
    ): ErrorsOr[Option[UUID]] =
    modifierOptionId match {
      case Some(mId) if modifierOptions.exists(_.id == mId) => Multiple.successOpt(mId)
      case Some(mId)                                        => Multiple.failure(InvalidModifierOptionIds(Seq(mId)))
      case None                                             => Multiple.failure(EmptyModifierOptionIds(Seq.empty))
    }
}

final case class RecoveredOrderItemModifierOptionUpsertion(
    modifierOptionId: Option[UUID],
    name: String,
    modifierSetName: Option[String],
    `type`: ModifierSetType,
    price: MonetaryAmount,
    quantity: BigDecimal,
  )
