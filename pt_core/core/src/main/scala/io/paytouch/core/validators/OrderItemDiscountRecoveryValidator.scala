package io.paytouch.core.validators

import java.util.UUID

import io.paytouch.core.data.daos.{ Daos, OrderItemDiscountDao }
import io.paytouch.core.data.model.OrderItemDiscountRecord
import io.paytouch.core.entities.{ ItemDiscountUpsertion, UserContext }
import io.paytouch.core.errors.{ InvalidOrderItemDiscountIds, NonAccessibleOrderItemDiscountIds }
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils.{ Multiple, PaytouchLogger }

import scala.concurrent._

class OrderItemDiscountRecoveryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends ItemDiscountRecoveryValidator[OrderItemDiscountRecord] {

  type Record = OrderItemDiscountRecord
  type Dao = OrderItemDiscountDao

  protected val dao = daos.orderItemDiscountDao
  val validationErrorF = InvalidOrderItemDiscountIds(_)
  val accessErrorF = NonAccessibleOrderItemDiscountIds(_)
  val errorMsg = accessErrorF

  def validateUpsertions(
      discountsPerItemId: Map[UUID, Seq[ItemDiscountUpsertion]],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[ItemDiscountUpsertion]]] = {
    val itemIds = discountsPerItemId.values.flatten.flatMap(_.id).toSeq
    val discountIds = discountsPerItemId.values.flatten.flatMap(_.discountId).toSeq
    validIdsInUpsertion(itemIds, discountIds) { (validItemDiscountIds, discounts) =>
      Multiple.sequence(discountsPerItemId.map {
        case (itemId, upsertions) =>
          validateItemDiscountUpsertion(upsertions, validItemDiscountIds, discounts)
      })
    }
  }

  def recoverUpsertionsPerItem(
      discountsPerItemId: Map[UUID, Seq[ItemDiscountUpsertion]],
    )(implicit
      user: UserContext,
    ): Future[Map[UUID, Seq[RecoveredItemDiscountUpsertion]]] = {
    val itemIds = discountsPerItemId.values.flatten.flatMap(_.id).toSeq
    val discountIds = discountsPerItemId.values.flatten.flatMap(_.discountId).toSeq
    validIdsInUpsertion(itemIds, discountIds) { (validItemDiscountIds, discounts) =>
      discountsPerItemId.map {
        case (itemId, upsertions) =>
          val recoveredUpsertions = toRecoveredItemDiscountUpsertion(upsertions, validItemDiscountIds, discounts)
          itemId -> recoveredUpsertions
      }
    }
  }
}
