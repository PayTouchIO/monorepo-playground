package io.paytouch.core.services

import io.paytouch.core.data.daos.{ Daos, OrderItemVariantOptionDao }
import io.paytouch.core.data.model.{ OrderItemVariantOptionRecord, OrderItemVariantOptionUpdate }
import io.paytouch.core.entities.{ OrderItemVariantOption, UserContext }
import io.paytouch.core.services.features.OrderItemRelationService
import io.paytouch.core.validators.RecoveredOrderItemUpsertion

import scala.concurrent.ExecutionContext

class OrderItemVariantOptionService(implicit val ec: ExecutionContext, val daos: Daos)
    extends OrderItemRelationService {

  type Dao = OrderItemVariantOptionDao
  type Entity = OrderItemVariantOption
  type Record = OrderItemVariantOptionRecord

  protected val dao = daos.orderItemVariantOptionDao

  def fromRecordToEntity(record: Record)(implicit user: UserContext): Entity =
    OrderItemVariantOption(
      id = record.id,
      orderItemId = record.orderItemId,
      variantOptionId = record.variantOptionId,
      optionName = record.optionName,
      optionTypeName = record.optionTypeName,
      position = record.position,
    )

  def convertToOrderItemVariantOptionUpdates(
      upsertion: RecoveredOrderItemUpsertion,
    )(implicit
      user: UserContext,
    ): Seq[OrderItemVariantOptionUpdate] = {
    val orderItemId = upsertion.id
    upsertion.variantOptions.map { variantOptionUpsertion =>
      OrderItemVariantOptionUpdate(
        id = None,
        merchantId = Some(user.merchantId),
        orderItemId = Some(orderItemId),
        variantOptionId = variantOptionUpsertion.variantOptionId,
        optionName = Some(variantOptionUpsertion.optionName),
        optionTypeName = Some(variantOptionUpsertion.optionTypeName),
        position = Some(variantOptionUpsertion.position),
      )
    }
  }

}
