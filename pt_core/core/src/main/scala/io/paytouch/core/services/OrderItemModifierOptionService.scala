package io.paytouch.core.services

import io.paytouch.core.data.daos.{ Daos, OrderItemModifierOptionDao }
import io.paytouch.core.data.model.{ OrderItemModifierOptionRecord, OrderItemModifierOptionUpdate }
import io.paytouch.core.entities.{ MonetaryAmount, OrderItemModifierOption, UserContext }
import io.paytouch.core.services.features.OrderItemRelationService
import io.paytouch.core.validators.RecoveredOrderItemUpsertion

import scala.concurrent.ExecutionContext

class OrderItemModifierOptionService(implicit val ec: ExecutionContext, val daos: Daos)
    extends OrderItemRelationService {

  type Dao = OrderItemModifierOptionDao
  type Entity = OrderItemModifierOption
  type Record = OrderItemModifierOptionRecord

  protected val dao = daos.orderItemModifierOptionDao

  def fromRecordToEntity(record: Record)(implicit user: UserContext): Entity =
    OrderItemModifierOption(
      id = record.id,
      orderItemId = record.orderItemId,
      modifierOptionId = record.modifierOptionId,
      name = record.name,
      modifierSetName = record.modifierSetName,
      `type` = record.`type`,
      price = MonetaryAmount(record.priceAmount, user.currency),
      quantity = record.quantity,
    )

  def convertToOrderItemModifierOptionUpdates(
      upsertion: RecoveredOrderItemUpsertion,
    )(implicit
      user: UserContext,
    ): Seq[OrderItemModifierOptionUpdate] = {
    val orderItemId = upsertion.id
    upsertion.modifierOptions.map { modifierOptionUpsertion =>
      OrderItemModifierOptionUpdate(
        id = None,
        merchantId = Some(user.merchantId),
        orderItemId = Some(orderItemId),
        modifierOptionId = modifierOptionUpsertion.modifierOptionId,
        name = Some(modifierOptionUpsertion.name),
        modifierSetName = modifierOptionUpsertion.modifierSetName,
        `type` = Some(modifierOptionUpsertion.`type`),
        priceAmount = Some(modifierOptionUpsertion.price.amount),
        quantity = Some(modifierOptionUpsertion.quantity),
      )
    }
  }

}
