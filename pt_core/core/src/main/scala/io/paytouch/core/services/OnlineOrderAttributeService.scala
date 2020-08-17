package io.paytouch.core.services

import java.time.{ LocalTime, ZoneId, ZonedDateTime }
import java.util.UUID

import scala.concurrent._

import cats.data._
import cats.implicits._

import io.paytouch.implicits._

import io.paytouch.core.calculations.OrderPreperationTimeCalculations
import io.paytouch.core.conversions.OnlineOrderAttributeConversions
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.{ OnlineOrderAttributeRecord, OnlineOrderAttributeUpdate, OrderRecord }
import io.paytouch.core.data.model.enums.{ AcceptanceStatus, Source }
import io.paytouch.core.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.ResultType
import io.paytouch.core.utils.UtcTime
import io.paytouch.core.utils.Multiple._
import io.paytouch.core.validators.{ OnlineOrderAttributeValidator, RecoveredOrderUpsertion }

class OnlineOrderAttributeService(
    val messageHandler: SQSMessageHandler,
    merchantService: => MerchantService,
    locationService: => LocationService,
    orderService: => OrderService,
    orderSyncService: => OrderSyncService,
    storeService: StoreService,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends OnlineOrderAttributeConversions
       with OrderPreperationTimeCalculations {
  type Entity = OnlineOrderAttribute
  type Record = OnlineOrderAttributeRecord
  type Validator = OnlineOrderAttributeValidator

  protected val dao = daos.onlineOrderAttributeDao
  val validator = new OnlineOrderAttributeValidator

  def enrich(records: Seq[Record])(implicit user: UserContext): Future[Seq[Entity]] =
    Future.successful(fromRecordsToEntities(records))

  def findAllByOrders(
      orderRecords: Seq[OrderRecord],
    )(implicit
      user: UserContext,
    ): Future[Map[OrderRecord, OnlineOrderAttribute]] =
    findByIds(orderRecords.flatMap(_.onlineOrderAttributeId)).map { onlineOrderAttributes =>
      orderRecords.flatMap { orderRecord =>
        onlineOrderAttributes
          .find(_.id.pipe(orderRecord.onlineOrderAttributeId.contains))
          .map(orderRecord -> _)
      }.toMap
    }

  def findRecordByOrderId(orderId: UUID): Future[Option[Record]] =
    dao.findByOrderId(orderId)

  def findByIds(onlineOrderAttributeIds: Seq[UUID])(implicit user: UserContext): Future[Seq[Entity]] =
    dao.findByIds(onlineOrderAttributeIds).flatMap(enrich)

  def findByOptId(onlineOrderAttributeId: Option[UUID])(implicit user: UserContext): Future[Option[Entity]] =
    findByIds(onlineOrderAttributeId.toSeq).map(_.headOption)

  def accept(
      orderId: UUID,
      acception: OrderAcception,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Order]]] =
    validator.validateAccept(orderId).flatMapTraverse {
      _.fold(none[Order].pure[Future]) { order =>
        implicit val m: MerchantContext = user.toMerchantContext

        for {
          locationTimezone <-
            locationService
              .findTimezoneForLocationWithFallback(order.locationId)
          customerRequestedPrepareByTime <- findByOptId(order.onlineOrderAttributeId)
            .map(_.flatMap(_.prepareByTime))
          _ <- acceptOnlineOrderAttribute(
            order,
            acception.estimatedPrepTimeInMins,
            locationTimezone,
            customerRequestedPrepareByTime,
          )
          orderEntity <-
            orderService
              .enrich(order, orderSyncService.defaultFilters)(orderSyncService.defaultExpansions)
        } yield orderEntity.some
      }
    }

  def acceptAndThenSendAcceptanceStatusMessage(
      orderId: UUID,
      acception: OrderAcception,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Order]]] =
    accept(orderId, acception).flatMapTraverse { order =>
      order
        .traverse(sendAcceptanceStatusMessage(_, AcceptanceStatus.Accepted))
        .as(order)
    }

  private def sendAcceptanceStatusMessage(
      orderEntity: Order,
      acceptanceStatus: AcceptanceStatus,
    )(implicit
      user: UserContext,
    ): Future[Unit] = {
    implicit val m: MerchantContext = user.toMerchantContext

    messageHandler.sendOrderUpdatedMsg(orderEntity)

    orderEntity.source match {
      case Some(Source.Storefront) =>
        sendStorefrontAcceptanceStatusMessage(orderEntity, acceptanceStatus)

      case Some(Source.DeliveryProvider) =>
        sendDeliveryAcceptanceStatusMessage(orderEntity, acceptanceStatus)

      case _ =>
        Future.unit
    }
  }

  private def sendStorefrontAcceptanceStatusMessage(
      orderEntity: Order,
      acceptanceStatus: AcceptanceStatus,
    )(implicit
      user: UserContext,
      merchant: MerchantContext,
    ): Future[Unit] = {
    val optT = for {
      recipientEmail <- OptionT.fromOption[Future](orderEntity.customer.flatMap(_.email))
      receiptContext <- OptionT(merchantService.prepareReceiptContext(recipientEmail, orderEntity))
    } yield acceptanceStatus match {
      case AcceptanceStatus.Accepted =>
        messageHandler.sendOrderAcceptedEmail(receiptContext)
      case AcceptanceStatus.Rejected =>
        messageHandler.sendOrderRejectedEmail(receiptContext)
      case _ => ()
    }

    optT.value.map(_.getOrElse((): Unit))
  }

  private def sendDeliveryAcceptanceStatusMessage(
      orderEntity: Order,
      acceptanceStatus: AcceptanceStatus,
    )(implicit
      merchant: MerchantContext,
    ): Future[Unit] =
    Future {
      acceptanceStatus match {
        case AcceptanceStatus.Accepted =>
          messageHandler.sendDeliveryOrderAccepted(orderEntity)
        case AcceptanceStatus.Rejected =>
          messageHandler.sendDeliveryOrderRejected(orderEntity)
        case _ => ()
      }
    }

  def reject(
      orderId: UUID,
      rejectionReason: OrderRejection,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Option[Order]]] =
    validator.validateReject(orderId).flatMapTraverse {
      case Some(order) =>
        for {
          _ <- rejectOnlineOrderAttribute(order, rejectionReason.rejectionReason)
          orderEntity <- orderService.rejectOrder(order)
          _ <- sendAcceptanceStatusMessage(orderEntity, AcceptanceStatus.Rejected)
        } yield Some(orderEntity)
      case _ => Future.successful(None)
    }

  def convertToOnlineOrderAttributeUpdate(
      upsertion: RecoveredOrderUpsertion,
    )(implicit
      user: UserContext,
    ): Future[Option[OnlineOrderAttributeUpdate]] =
    locationService
      .findTimezoneForLocationWithFallback(upsertion.locationId)(user.toMerchantContext)
      .map { zoneId =>
        upsertion.onlineOrderAttribute.map { ooa =>
          val calculatedPrepareByDateTime = ooa
            .prepareByTime
            .map(interpretPrepareByTimeAsDateTimeInUTC(UtcTime.now, _, zoneId))
            .flatten
          val updatedOoa = ooa.copy(prepareByDateTime = ooa.prepareByDateTime.getOrElse(calculatedPrepareByDateTime))
          toUpdate(updatedOoa)
        }
      }

  private def acceptOnlineOrderAttribute(
      order: OrderRecord,
      estimatedPrepTimeInMins: Option[Int],
      locationTimezone: ZoneId,
      customerRequestedPrepareByTime: Option[LocalTime],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Record)] =
    fetchEstimatedDrivingTimeInMins(order).flatMap { estimatedDrivingTimeInMins =>
      val acceptedAt = UtcTime.now

      val estimatedReadyAt =
        calculateEstimatedReadyAt(
          acceptedAt,
          estimatedPrepTimeInMins,
          estimatedDrivingTimeInMins,
          locationTimezone,
          customerRequestedPrepareByTime,
        )

      OnlineOrderAttributeUpdate
        .empty
        .copy(
          id = order.onlineOrderAttributeId,
          merchantId = order.merchantId.some,
          acceptanceStatus = AcceptanceStatus.Accepted.some,
          acceptedAt = acceptedAt.some,
          estimatedPrepTimeInMins = estimatedPrepTimeInMins,
          estimatedReadyAt = estimatedReadyAt,
          estimatedDeliveredAt = calculateEstimatedDeliveredAt(
            estimatedReadyAt,
            estimatedDrivingTimeInMins,
          ),
        )
        .pipe(dao.upsert)
    }

  private def rejectOnlineOrderAttribute(
      order: OrderRecord,
      rejectionReason: Option[String],
    )(implicit
      user: UserContext,
    ): Future[(ResultType, Record)] =
    OnlineOrderAttributeUpdate
      .empty
      .copy(
        id = order.onlineOrderAttributeId,
        merchantId = Some(order.merchantId),
        acceptanceStatus = Some(AcceptanceStatus.Rejected),
        rejectionReason = rejectionReason,
        rejectedAt = Some(UtcTime.now),
        estimatedReadyAt = Some(None),
        estimatedDeliveredAt = Some(None),
      )
      .pipe(dao.upsert)

  private def fetchEstimatedDrivingTimeInMins(order: OrderRecord): Future[Option[Int]] =
    order.deliveryAddressId match {
      case None => Future.successful(None)
      case Some(deliveryAddressId) =>
        daos.orderDeliveryAddressDao.findById(deliveryAddressId).map { result =>
          result.flatMap(_.estimatedDrivingTimeInMins)
        }
    }
}
