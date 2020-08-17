package io.paytouch.core.validators

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging

import cats.data.Validated.{ Invalid, Valid }
import cats.implicits._

import io.paytouch.core.data.daos.{ Daos, TicketDao }
import io.paytouch.core.data.model.{ KitchenRecord, LocationRecord, OrderRecord, TicketRecord }
import io.paytouch.core.entities.{ TicketUpdate, UserContext }
import io.paytouch.core.errors._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.utils._

import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent._

class TicketValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[TicketRecord]
       with LazyLogging {

  type Record = TicketRecord
  type Dao = TicketDao

  protected val dao = daos.ticketDao
  val validationErrorF = InvalidTicketIds(_)
  val accessErrorF = NonAccessibleTicketIds(_)

  val locationValidator = new LocationValidator
  val orderValidator = new OrderValidator
  val orderItemValidator = new OrderItemValidator

  val ticketOrderItemDao = daos.ticketOrderItemDao
  val kitchenDao = daos.kitchenDao

  def validateUpsertion(
      id: UUID,
      update: TicketUpdate,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[(TicketUpdate, Option[Record], KitchenRecord)]] =
    for {
      existingTicket <- dao.findById(id)
      existingTicketOrderItems <- ticketOrderItemDao.findByTicketId(id)
      locationId = update.locationId.orElse(existingTicket.map(_.locationId)).getOrElse(UUID.randomUUID)
      orderId = update.orderId.orElse(existingTicket.map(_.orderId)).getOrElse(UUID.randomUUID)
      orderItemIds = update.orderItemIds.getOrElse(existingTicketOrderItems.map(_.orderItemId))
      location <- locationValidator.accessOneById(locationId)
      order <- orderValidator.accessOneById(orderId)
      orderItems <- orderItemValidator.accessByIdsAndOrderId(orderItemIds, orderId)
      orderItemsNonEmpty <- validateOrderItemsNotEmpty(update.orderItemIds)
      orderBelongsToLocation <- validateOrderBelongsToLocation(order.toOption, location.toOption)
      kitchen <- validateKitchen(update, existingTicket, locationId)
    } yield Multiple.combine(location, order, orderItems, orderItemsNonEmpty, orderBelongsToLocation, kitchen) {
      case (_, _, _, _, _, k) => (update, existingTicket, k)
    }

  def validateKitchen(
      update: TicketUpdate,
      existingTicket: Option[Record],
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[KitchenRecord]] = {
    val kitchenId = update.routeToKitchenId.getOrElse(existingTicket.map(_.routeToKitchenId).getOrElse(UUID.randomUUID))

    kitchenDao.findDeletedById(kitchenId).map {
      case None => Multiple.failure(MatchingKitchenIdNotFound(update))
      case Some(kitchen) =>
        if (kitchen.locationId != locationId)
          Multiple.failure(KitchenLocationIdMismatch(kitchen.locationId, locationId))
        else {
          if (kitchen.deletedAt.isDefined)
            logger.warn(s"Syncing ticket for deleted kitchen ${kitchen.id}")
          Multiple.success(kitchen)
        }
    }
  }

  def validateOrderItemsNotEmpty(orderItemIds: Option[Seq[UUID]]): Future[ErrorsOr[Option[Seq[UUID]]]] =
    Future.successful {
      orderItemIds match {
        case Some(ids) if ids.isEmpty => Multiple.failure(InvalidTicketOrderItemsAssociation())
        case Some(ids)                => Multiple.successOpt(ids)
        case None                     => Multiple.empty
      }
    }

  def validateOrderBelongsToLocation(
      order: Option[OrderRecord],
      location: Option[LocationRecord],
    ): Future[ErrorsOr[Option[Unit]]] =
    Future.successful {
      (order, location) match {
        case (Some(ord), Some(loc)) if !ord.locationId.contains(loc.id) =>
          Multiple.failure(InvalidOrderLocationAssociation(orderId = ord.id, locationId = loc.id))
        case _ => Multiple.empty
      }
    }

}
