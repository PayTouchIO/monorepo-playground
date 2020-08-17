package io.paytouch.core.services.features

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickReceivingObjectDao
import io.paytouch.core.data.model.enums.{ ReceivingObjectStatus, ReceivingOrderObjectType, ReceivingOrderStatus }
import io.paytouch.core.data.model.{ ReceivingOrderRecord, SlickMerchantRecord, SlickReceivingObjectRecord }
import io.paytouch.core.services.ReceivingOrderService
import io.paytouch.core.utils.Implicits

import scala.collection.Map
import scala.concurrent._

trait ReceivingObjectUpdateService extends Implicits {

  type Dao <: SlickReceivingObjectDao

  protected def dao: Dao
  def receivingOrderService: ReceivingOrderService
  def objectType: ReceivingOrderObjectType

  def inferAndUpdateStatuses(objectId: UUID): Future[Unit] = {
    val receivingOrdersR = receivingOrderService.findPerReceivingObjectIdAndType(objectId, objectType)
    val missingQuantitiesR = dao.missingQuantitiesPerProductId(objectId)
    dao.findById(objectId).flatMap {
      case Some(objectRecord) =>
        for {
          receivingOrders <- receivingOrdersR
          missingQuantities <- missingQuantitiesR
          _ <- updateStatus(objectRecord, receivingOrders, missingQuantities)
          _ <- updatePaymentStatus(objectRecord, receivingOrders)
        } yield ()
      case _ => Future.unit
    }
  }

  private def updateStatus(
      objectRecord: Dao#Record,
      receivingOrders: Seq[ReceivingOrderRecord],
      missingQuantities: Map[UUID, BigDecimal],
    ): Future[Int] = {
    val status = (receivingOrders, missingQuantities) match {
      case (orders, _) if orders.isEmpty                                            => ReceivingObjectStatus.Created
      case (orders, _) if orders.forall(_.status == ReceivingOrderStatus.Receiving) => ReceivingObjectStatus.Receiving
      case (_, quantities) if quantities.isEmpty                                    => ReceivingObjectStatus.Completed
      case _                                                                        => ReceivingObjectStatus.Partial
    }
    dao.setStatus(objectRecord.id, status)
  }

  protected def updatePaymentStatus(objectRecord: Dao#Record, receivingOrders: Seq[ReceivingOrderRecord]): Future[Int]
}
