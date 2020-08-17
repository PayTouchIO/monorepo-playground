package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ VariantOptionRecord, VariantOptionTypeRecord }
import io.paytouch.core.entities._
import io.paytouch.core.errors.{
  EmptyVariantOptionIds,
  InvalidVariantOptionIds,
  OrderSyncMissingOptionName,
  OrderSyncMissingOptionTypeName,
}

import io.paytouch.core.utils.Multiple._

import io.paytouch.core.utils.{ Multiple, PaytouchLogger }

import scala.concurrent._

class OrderItemVariantOptionValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) {

  val variantOptionValidator = new VariantOptionValidator
  val variantOptionTypeDao = daos.variantOptionTypeDao

  def validateUpsertions(
      variantOptionsPerOrderId: Map[UUID, Seq[OrderItemVariantOptionUpsertion]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[OrderItemVariantOptionUpsertion]]] = {
    val variantOptionIds = variantOptionsPerOrderId.values.flatten.flatMap(_.variantOptionId).toSeq
    for {
      variantOptions <- variantOptionValidator.filterValidByIds(variantOptionIds)
      variantOptionTypeIds = variantOptions.map(_.variantOptionTypeId)
      variantOptionTypes <- variantOptionTypeDao.findByIds(variantOptionTypeIds)
    } yield Multiple.sequence(
      variantOptionsPerOrderId.map {
        case (orderItemId, upsertions) =>
          Multiple.combineSeq(upsertions.map { upsertion =>
            val validVariantOptionId = recoverVariantOptionId(variantOptions, upsertion.variantOptionId)
            val validOptionName = validateVariantOptionName(upsertion, variantOptions, upsertion.optionName)
            val validOptionTypeName =
              validateVariantOptionTypeName(upsertion, variantOptions, variantOptionTypes, upsertion.optionTypeName)
            Multiple.combine(validVariantOptionId, validOptionName, validOptionTypeName) {
              case _ => upsertion
            }
          })
      },
    )
  }

  def recoverUpsertions(
      variantOptionsPerOrderId: Map[UUID, Seq[OrderItemVariantOptionUpsertion]],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[RecoveredOrderItemVariantOptionUpsertion]]] = {
    val variantOptionIds = variantOptionsPerOrderId.values.flatten.flatMap(_.variantOptionId).toSeq
    for {
      variantOptions <- variantOptionValidator.filterValidByIds(variantOptionIds)
      variantOptionTypeIds = variantOptions.map(_.variantOptionTypeId)
      variantOptionTypes <- variantOptionTypeDao.findByIds(variantOptionTypeIds)
    } yield variantOptionsPerOrderId.map {
      case (orderItemId, upsertions) =>
        val recoveredUpsertions = upsertions.zipWithIndex.flatMap {
          case (upsertion, position) =>
            val recoveredVariantOptionId =
              logger.loggedSoftRecover(recoverVariantOptionId(variantOptions, upsertion.variantOptionId))(
                "Variant option not found, assuming it has been deleted",
              )
            val recoveredOptionName = recoverVariantOptionName(upsertion, variantOptions, upsertion.optionName)
            val recoveredOptionTypeName =
              recoverVariantOptionTypeName(upsertion, variantOptions, variantOptionTypes, upsertion.optionTypeName)
            for {
              optionName <- recoveredOptionName
              optionTypeName <- recoveredOptionTypeName
            } yield RecoveredOrderItemVariantOptionUpsertion(
              variantOptionId = recoveredVariantOptionId,
              optionName = optionName,
              optionTypeName = optionTypeName,
              position = upsertion.position.getOrElse(position),
            )
        }
        orderItemId -> recoveredUpsertions
    }
  }

  private def recoverVariantOptionId(
      variantOptions: Seq[VariantOptionRecord],
      variantOptionId: Option[UUID],
    ): ErrorsOr[Option[UUID]] =
    variantOptionId match {
      case Some(vId) if variantOptions.exists(_.id == vId) => Multiple.successOpt(vId)
      case Some(vId)                                       => Multiple.failure(InvalidVariantOptionIds(Seq(vId)))
      case None                                            => Multiple.failure(EmptyVariantOptionIds(Seq.empty))
    }

  private def recoverVariantOptionName(
      upsertion: OrderItemVariantOptionUpsertion,
      variantOptions: Seq[VariantOptionRecord],
      optionName: Option[String],
    ): Option[String] =
    optionName match {
      case Some(_) => optionName
      case None =>
        val existingOptionName = variantOptions.find(vo => upsertion.variantOptionId.contains(vo.id)).map(_.name)
        val description = existingOptionName.fold(
          s"No variant option with id ${upsertion.variantOptionId}, no recovery possible. Skipping this record.",
        )(n => s"Found name $n in the db")
        logger.loggedGenericRecover(
          Multiple.failure[Option[String]](OrderSyncMissingOptionName(upsertion)),
          existingOptionName,
        )(description, upsertion)
    }

  private def validateVariantOptionName(
      upsertion: OrderItemVariantOptionUpsertion,
      variantOptions: Seq[VariantOptionRecord],
      optionName: Option[String],
    ): ErrorsOr[Option[String]] =
    optionName match {
      case Some(name) => Multiple.successOpt(name)
      case None =>
        val existingOptionName = variantOptions.find(vo => upsertion.variantOptionId.contains(vo.id)).map(_.name)
        existingOptionName match {
          case Some(name) => Multiple.successOpt(name)
          case None       => Multiple.failure(OrderSyncMissingOptionName(upsertion))
        }
    }

  private def recoverVariantOptionTypeName(
      upsertion: OrderItemVariantOptionUpsertion,
      variantOptions: Seq[VariantOptionRecord],
      variantOptionTypes: Seq[VariantOptionTypeRecord],
      optionTypeName: Option[String],
    ): Option[String] =
    optionTypeName match {
      case Some(_) => optionTypeName
      case None =>
        val existingOptionTypeName = {
          val variantOptionTypeId =
            variantOptions.find(vo => upsertion.variantOptionId.contains(vo.id)).map(_.variantOptionTypeId)
          variantOptionTypes.find(vot => variantOptionTypeId.contains(vot.id)).map(_.name)
        }
        val description = existingOptionTypeName.fold(
          s"No variant option with id ${upsertion.variantOptionId}, no recovery possible. Skipping this record.",
        )(n => s"Found name $n in the db.")
        logger.loggedGenericRecover(
          Multiple
            .failure[Option[String]](OrderSyncMissingOptionTypeName(upsertion)),
          existingOptionTypeName,
        )(description, upsertion)
    }

  private def validateVariantOptionTypeName(
      upsertion: OrderItemVariantOptionUpsertion,
      variantOptions: Seq[VariantOptionRecord],
      variantOptionTypes: Seq[VariantOptionTypeRecord],
      optionTypeName: Option[String],
    ): ErrorsOr[Option[String]] =
    optionTypeName match {
      case Some(name) => Multiple.successOpt(name)
      case None =>
        val existingOptionTypeName = {
          val variantOptionTypeId =
            variantOptions.find(vo => upsertion.variantOptionId.contains(vo.id)).map(_.variantOptionTypeId)
          variantOptionTypes.find(vot => variantOptionTypeId.contains(vot.id)).map(_.name)
        }

        existingOptionTypeName match {
          case Some(name) => Multiple.successOpt(name)
          case None       => Multiple.failure(OrderSyncMissingOptionTypeName(upsertion))
        }
    }
}

final case class RecoveredOrderItemVariantOptionUpsertion(
    variantOptionId: Option[UUID],
    optionName: String,
    optionTypeName: String,
    position: Int,
  )
