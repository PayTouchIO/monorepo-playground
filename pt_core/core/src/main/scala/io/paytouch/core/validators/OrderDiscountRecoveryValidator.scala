package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, OrderDiscountDao }
import io.paytouch.core.data.model.OrderDiscountRecord
import io.paytouch.core.entities.{ ItemDiscountUpsertion, UserContext }
import io.paytouch.core.errors.{ InvalidOrderDiscountIds, NonAccessibleOrderDiscountIds }
import io.paytouch.core.utils.{ Multiple, PaytouchLogger }
import io.paytouch.core.validators.features.DefaultValidator
import io.paytouch.core.utils.Multiple.ErrorsOr

import scala.concurrent._

class OrderDiscountRecoveryValidator(
    implicit
    val ec: ExecutionContext,
    val daos: Daos,
    val logger: PaytouchLogger,
  ) extends ItemDiscountRecoveryValidator[OrderDiscountRecord] {

  type Record = OrderDiscountRecord
  type Dao = OrderDiscountDao

  protected val dao = daos.orderDiscountDao
  val validationErrorF = InvalidOrderDiscountIds(_)
  val accessErrorF = NonAccessibleOrderDiscountIds(_)
  val errorMsg = accessErrorF

  def validateUpsertions(
      upsertions: Seq[ItemDiscountUpsertion],
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[ItemDiscountUpsertion]]] = {
    val itemIds = upsertions.flatMap(_.id)
    val discountIds = upsertions.flatMap(_.discountId)
    validIdsInUpsertion(itemIds, discountIds) { (validItemDiscountIds, discounts) =>
      validateItemDiscountUpsertion(upsertions, validItemDiscountIds, discounts)
    }
  }

  def recoverUpsertions(
      upsertions: Seq[ItemDiscountUpsertion],
    )(implicit
      user: UserContext,
    ): Future[Seq[RecoveredItemDiscountUpsertion]] = {
    val itemIds = upsertions.flatMap(_.id)
    val discountIds = upsertions.flatMap(_.discountId)
    validIdsInUpsertion(itemIds, discountIds) { (validItemDiscountIds, discounts) =>
      toRecoveredItemDiscountUpsertion(upsertions, validItemDiscountIds, discounts)
    }
  }
}
