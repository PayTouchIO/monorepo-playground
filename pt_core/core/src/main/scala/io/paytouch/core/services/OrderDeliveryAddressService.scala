package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.conversions.OrderDeliveryAddressConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ OrderDeliveryAddressRecord, OrderDeliveryAddressUpdate, OrderRecord }
import io.paytouch.core.entities.{ DeliveryAddress, UserContext }
import io.paytouch.core.validators.RecoveredOrderUpsertion

import scala.concurrent._

class OrderDeliveryAddressService(implicit val ec: ExecutionContext, val daos: Daos)
    extends OrderDeliveryAddressConversions {

  type Entity = DeliveryAddress
  type Record = OrderDeliveryAddressRecord
  type Update = OrderDeliveryAddressUpdate

  protected val dao = daos.orderDeliveryAddressDao

  def enrich(records: Seq[Record]): Future[Seq[Entity]] = Future.successful(fromRecordsToEntities(records))

  def findAllByOrders(orderRecords: Seq[OrderRecord]): Future[Map[OrderRecord, DeliveryAddress]] = {
    val deliveryAddressIds = orderRecords.flatMap(_.deliveryAddressId)
    findByIds(deliveryAddressIds).map { deliveryAddresses =>
      orderRecords.flatMap { orderRecord =>
        val maybeDeliveryAddress = deliveryAddresses.find(da => orderRecord.deliveryAddressId.contains(da.id))
        maybeDeliveryAddress.map(da => orderRecord -> da)
      }.toMap
    }
  }

  def findByIds(deliveryAddressIds: Seq[UUID]): Future[Seq[Entity]] =
    dao.findByIds(deliveryAddressIds).flatMap(enrich)

  def convertToOrderDeliveryAddressUpdate(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[OrderDeliveryAddressUpdate]] =
    Future.successful {
      upsertion.deliveryAddress.map(toUpdate)
    }
}
