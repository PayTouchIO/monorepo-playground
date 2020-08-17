package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import akka.actor.ActorRef

import cats.implicits._

import io.paytouch.core._
import io.paytouch.core.async.monitors._
import io.paytouch.core.conversions.TicketConversions
import io.paytouch.core.data._
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model.upsertions.TicketUpsertion
import io.paytouch.core.expansions._
import io.paytouch.core.filters.TicketFilters
import io.paytouch.core.services.features._
import io.paytouch.core.utils._
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.TicketValidator

class TicketService(
    val monitor: ActorRef withTag TicketMonitor,
    val orderItemService: OrderItemService,
    val merchantService: MerchantService,
    val onlineOrderAttributeService: OnlineOrderAttributeService,
    kitchenService: => KitchenService,
    orderService: => OrderService,
    ticketOrderItemService: => TicketOrderItemService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends TicketConversions
       with FindAllFeature
       with FindByIdFeature
       with CreateAndUpdateFeatureWithStateProcessing {

  type Creation = entities.TicketCreation
  type Dao = TicketDao
  type Entity = entities.Ticket
  type Expansions = TicketExpansions
  type Filters = TicketFilters
  type Model = TicketUpsertion
  type Record = model.TicketRecord
  type Update = entities.TicketUpdate
  type Validator = TicketValidator

  type State = Seq[entities.OrderItem]

  protected val dao = daos.ticketDao
  protected val validator = new TicketValidator

  val defaultFilters = TicketFilters()

  def findByOrderId(orderId: UUID): Future[Seq[Record]] =
    dao.findPerOrderIds(Seq(orderId)).map(_.getOrElse(orderId, Seq.empty))

  def findByOrders(orders: Seq[model.OrderRecord]): Future[Map[model.OrderRecord, Seq[Record]]] =
    dao.findPerOrderIds(orders.map(_.id)).map(_.mapKeysToRecords(orders))

  def findByOrderItems(orderItems: Seq[model.OrderItemRecord]): Future[Map[model.OrderItemRecord, Seq[Record]]] =
    dao.findPerOrderItemIds(orderItems.map(_.id)).map(_.mapKeysToRecords(orderItems))

  def enrich(
      records: Seq[Record],
      f: Filters,
    )(
      e: Expansions,
    )(implicit
      user: entities.UserContext,
    ): Future[Seq[Entity]] = {
    val orderItemsPerTicketR = getOrderItemsPerTicket(records)
    val bundleOrderItemsPerOrderIdR = getBundleOrderItemsPerOrderId(records)
    val orderTicketR = getOptionalOrderPerTicket(records)(e.withOrder)
    val kitchenR =
      kitchenService.getKitchensMap()
    for {
      orderItemsPerTicket <- orderItemsPerTicketR
      bundleOrderItemsPerOrderId <- bundleOrderItemsPerOrderIdR
      orderPerTicket <- orderTicketR
      kitchens <- kitchenR
    } yield fromRecordsAndOptionsToEntities(
      records,
      orderItemsPerTicket,
      bundleOrderItemsPerOrderId,
      orderPerTicket,
      kitchens,
    )
  }

  private def getOrderItemsPerTicket(
      items: Seq[Record],
    )(implicit
      user: entities.UserContext,
    ): Future[Map[Record, Seq[entities.OrderItem]]] =
    ticketOrderItemService.getOrderItemsPerTickets(items)

  private def getBundleOrderItemsPerOrderId(
      items: Seq[Record],
    )(implicit
      user: entities.UserContext,
    ): Future[Map[UUID, Seq[entities.OrderItem]]] =
    ticketOrderItemService.getBundleOrderItemsPerOrderId(items)

  private def getOptionalOrderPerTicket(
      items: Seq[Record],
    )(
      withOrder: Boolean,
    )(implicit
      user: entities.UserContext,
    ): Future[DataByRecord[entities.Order]] =
    if (withOrder) {
      val orderIds = items.map(_.orderId)
      orderService.findByIds(orderIds)(OrderExpansions.empty).map { orders =>
        val ordersPerItem = items.flatMap { item =>
          val order = orders.filter(_.id == item.orderId)
          order.map(o => item -> o)
        }.toMap
        Some(ordersPerItem)
      }
    }
    else Future.successful(None)

  protected def convertToUpsertionModel(
      id: UUID,
      update: Update,
    )(implicit
      user: entities.UserContext,
    ): Future[ErrorsOr[Model]] =
    for {
      ticket <- convertToTicketUpdate(id, update)
      ticketOrderItems <- ticketOrderItemService.convertToTicketOrderItemUpdates(id, update)
    } yield Multiple.combine(ticket, ticketOrderItems)(TicketUpsertion)

  private def convertToTicketUpdate(
      id: UUID,
      update: Update,
    )(implicit
      user: entities.UserContext,
    ): Future[ErrorsOr[model.TicketUpdate]] =
    validator.validateUpsertion(id, update).mapNested {
      case (validUpdate, maybeExistingTicket, kitchen) =>
        val modelUpdate = fromUpsertionToUpdate(id, validUpdate)

        val previousStatus = maybeExistingTicket.map(_.status)
        val targetStatus = validUpdate.status.orElse(previousStatus)

        val now = UtcTime.now
        import entities.enums.TicketStatus._

        def setAsStarted = (m: model.TicketUpdate) => m.copy(startedAt = Some(now))
        def setAsCompleted =
          (m: model.TicketUpdate) => m.copy(completedAt = Some(now), show = Some(false), status = Some(Completed))
        def autoComplete = setAsStarted.andThen(setAsCompleted)

        val applyStatusTransitionEffects: model.TicketUpdate => model.TicketUpdate =
          (previousStatus, targetStatus) match {
            case (e, t) if e == t                      => identity
            case (_, Some(New)) if !kitchen.kdsEnabled => autoComplete
            case (_, Some(InProgress))                 => setAsStarted
            case (Some(InProgress), Some(Completed))   => setAsCompleted
            case (Some(New), Some(Completed))          => autoComplete
            case _                                     => identity
          }

        applyStatusTransitionEffects(modelUpdate)
    }

  protected def saveCreationState(
      id: UUID,
      creation: Creation,
    )(implicit
      user: entities.UserContext,
    ): Future[Option[State]] =
    orderItemService.findByIds(creation.orderItemIds).map(Some(_))

  protected def saveCurrentState(record: Record)(implicit user: entities.UserContext): Future[State] =
    getOrderItemsPerTicket(Seq(record)).map(orderItemsPerTicket => orderItemsPerTicket.getOrElse(record, Seq.empty))

  protected def processChangeOfState(
      state: Option[State],
      update: Update,
      resultType: ResultType,
      entity: Entity,
    )(implicit
      user: entities.UserContext,
    ): Future[Unit] = {
    orderService.postTicketUpsert(entity.orderId, entity.locationId)
    state.foreach(s => monitor ! TicketChange(s, update, resultType, entity, user))

    Future.unit
  }

  implicit def toFutureResultTypeEntity(
      f: Future[(ResultType, Record)],
    )(implicit
      user: entities.UserContext,
    ): Future[(ResultType, entities.Ticket)] =
    f.flatMap {
      case (resultType, record) =>
        enrich(record, defaultFilters)(TicketExpansions(withOrder = true)).map(e => (resultType, e))
    }

  def postOrderUpsertActions(order: model.OrderRecord)(implicit user: entities.UserContext): Future[Unit] = {
    def pretty(tickets: Seq[model.TicketRecord]): Seq[(UUID, entities.enums.TicketStatus)] =
      tickets.map(t => t.id -> t.status)

    def cancelMany(tickets: Seq[model.TicketRecord], show: Boolean): Future[Seq[ErrorsOr[Result[entities.Ticket]]]] = {
      logger.debug(s"[AUTOCOMPLETION] CANCELING TICKETS ${pretty(tickets)} FOR ORDER ID ${order.id}")

      Future.sequence(tickets.map(cancelTicket(show)))
    }

    def completeMany(tickets: Seq[model.TicketRecord]): Future[Seq[ErrorsOr[Result[entities.Ticket]]]] = {
      logger.debug(s"[AUTOCOMPLETION] COMPLETING TICKETS ${pretty(tickets)} FOR ORDER ID ${order.id}")

      Future.sequence(tickets.map(completeTicket))
    }

    def cancelTicket(show: Boolean)(ticket: model.TicketRecord): Future[ErrorsOr[Result[entities.Ticket]]] =
      update(ticket.id, toTicketUpdateEntity(entities.enums.TicketStatus.Canceled, show))

    def completeTicket(ticket: model.TicketRecord): Future[ErrorsOr[Result[entities.Ticket]]] =
      update(ticket.id, toTicketUpdateEntity(entities.enums.TicketStatus.Completed, show = false))

    order.status.fold(().pure[Future]) {
      case model.enums.OrderStatus.Canceled =>
        for {
          merchant <- merchantService.findById(order.merchantId)(MerchantExpansions.none)
          onlineOrderAttribute <- onlineOrderAttributeService.findByOptId(order.onlineOrderAttributeId)
          tickets <- dao.findNonCompletedByOrderId(order.id)
          _ <- merchant.map(_.setupType).fold(().pure[Future]) {
            case model.enums.SetupType.Paytouch =>
              cancelMany(tickets, show = false).void

            case model.enums.SetupType.Dash =>
              val isAcknowledged: Boolean =
                onlineOrderAttribute
                  .flatMap(_.cancellationStatus)
                  .contains(model.enums.CancellationStatus.Acknowledged)

              if (isAcknowledged)
                cancelMany(tickets, show = false).void
              else
                cancelMany(tickets, show = true).void

            case _ =>
              ().pure[Future]
          }
        } yield ()

      case model.enums.OrderStatus.Completed =>
        for {
          tickets <- dao.findNonCompletedByOrderId(order.id)
          _ <- completeMany(tickets.filter(_.status == entities.enums.TicketStatus.New))
        } yield ()

      case _ =>
        ().pure[Future]
    }
  }
}
