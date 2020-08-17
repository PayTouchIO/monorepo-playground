package io.paytouch.core.processors

import java.util.UUID

import cats.data.OptionT
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.data.daos.Daos
import io.paytouch.core.entities.{ LoyaltyMembership, LoyaltyProgram, Order, UserContext }
import io.paytouch.core.expansions.MerchantExpansions
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.messages.entities.{ PrepareOrderReceipt, SQSMessage }
import io.paytouch.core.services._

import scala.concurrent._

class PrepareOrderReceiptProcessor(
    val loyaltyMembershipService: LoyaltyMembershipService,
    val locationEmailReceiptService: LocationEmailReceiptService,
    val locationReceiptService: LocationReceiptService,
    val loyaltyProgramService: LoyaltyProgramService,
    val merchantService: MerchantService,
    val messageHandler: SQSMessageHandler,
  )(implicit
    val ec: ExecutionContext,
    val daos: Daos,
  ) extends Processor
       with LazyLogging {

  def execute: PartialFunction[SQSMessage[_], Future[Unit]] = {
    case msg: PrepareOrderReceipt => processPrepareOrderReceipt(msg)
  }

  private def processPrepareOrderReceipt(msg: PrepareOrderReceipt): Future[Unit] = {
    val merchantId = msg.payload.merchantId
    val recipientEmail = msg.payload.recipientEmail
    val paymentTransactionId = msg.payload.paymentTransactionId
    implicit val user = msg.payload.userContext
    implicit val merchant = user.toMerchantContext
    val order = msg.payload.data
    for {
      loyaltyMembership <- findOrCreateLoyaltyMembership(merchantId, order)
      loyaltyProgram <- loyaltyProgramService.findByOptId(loyaltyMembership.map(_.loyaltyProgramId))
    } yield sendOrderReceiptMessage(order, paymentTransactionId, recipientEmail, loyaltyMembership, loyaltyProgram)
  }

  private def findOrCreateLoyaltyMembership(
      merchantId: UUID,
      order: Order,
    )(implicit
      user: UserContext,
    ): Future[Option[LoyaltyMembership]] =
    order.customer match {
      case Some(customer) =>
        loyaltyMembershipService.findOrCreateInActiveProgram(customer.id, order.location.map(_.id))
      case None => Future.successful(None)
    }

  private def sendOrderReceiptMessage(
      order: Order,
      paymentTransactionId: Option[UUID],
      recipientEmail: String,
      loyaltyMembership: Option[LoyaltyMembership],
      loyaltyProgram: Option[LoyaltyProgram],
    )(implicit
      user: UserContext,
    ): Future[Unit] = {
    val merchantId = user.merchantId
    val loyaltyMembershipWithOrderIdContext =
      loyaltyMembershipService.updateLinksWithOrderId(loyaltyMembership, order.id)
    val optT = for {
      merchant <- OptionT(merchantService.findById(merchantId)(MerchantExpansions.none))
      locationId <- OptionT.fromOption[Future](order.location.map(_.id))
      locationEmailReceipt <- OptionT(locationEmailReceiptService.findByLocationId(locationId))
      locationReceipt <- OptionT(locationReceiptService.findByLocationId(locationId))
    } yield messageHandler.sendOrderReceiptMsg(
      order,
      paymentTransactionId,
      recipientEmail,
      merchant,
      locationEmailReceipt,
      locationReceipt,
      loyaltyMembershipWithOrderIdContext,
      loyaltyProgram,
    )
    optT.value.map { result =>
      if (result.isEmpty)
        logger.error(
          s"Failed to send order receipt for merchantId={}, locationId={} (missing locationReceipt or locationEmailReceipt)",
          user.merchantId,
          order.location.map(_.id),
        )
    }
  }
}
