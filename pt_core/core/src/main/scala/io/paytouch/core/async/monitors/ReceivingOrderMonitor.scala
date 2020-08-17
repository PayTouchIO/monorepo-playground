package io.paytouch.core.async.monitors

import akka.actor.Actor
import cats.data.OptionT
import cats.instances.future._
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.ReceivingOrderRecord
import io.paytouch.core.data.model.enums.ReceivingOrderObjectType
import io.paytouch.core.entities.{ ReceivingOrder, UserContext }
import io.paytouch.core.services._

import scala.concurrent.ExecutionContext

final case class ReceivingOrderChange(
    state: Option[ReceivingOrderService#State],
    entity: ReceivingOrder,
    userContext: UserContext,
  )

class ReceivingOrderMonitor(
    val productService: ProductService,
    val receivingOrderService: ReceivingOrderService,
    val receivingOrderProductService: ReceivingOrderProductService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Actor {

  val receivingOrderDao = daos.receivingOrderDao

  def receive: Receive = {
    case ReceivingOrderChange(state, entity, user) =>
      OptionT(receivingOrderDao.findById(entity.id)).map { record =>
        receivingOrderService.updateReceivingObjectStatus(record)(user)
        updateAverageCost(record, state)
      }
  }

  private def updateAverageCost(
      receivingOrderRecord: ReceivingOrderRecord,
      state: Option[ReceivingOrderService#State],
    ) =
    receivingOrderProductService.findByReceivingOrderId(receivingOrderRecord.id).map { currentProducts =>
      val currentProductIds = currentProducts.map(_.productId)
      val previousProductIds =
        state.map { case (_, receivingOrderProducts) => receivingOrderProducts.map(_.productId) }.getOrElse(Seq.empty)
      val allProductIds = currentProductIds ++ previousProductIds
      productService.updateAverageCost(allProductIds.distinct, receivingOrderRecord.locationId)
    }
}
