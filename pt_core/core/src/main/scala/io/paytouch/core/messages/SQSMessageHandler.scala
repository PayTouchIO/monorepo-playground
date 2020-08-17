package io.paytouch.core.messages

import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem }
import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.entities._
import io.paytouch.core.messages.entities._
import io.paytouch.core.data.model
import io.paytouch.utils.Tagging._

class SQSMessageHandler(val asyncSystem: ActorSystem, val messageSender: ActorRef withTag SQSMessageSender) {

  def sendPrepareOrderReceiptMsg(
      entity: Order,
      paymentTransactionId: Option[UUID],
      email: String,
    )(implicit
      user: UserContext,
    ): Unit = {
    val msg = PrepareOrderReceipt(entity, paymentTransactionId, email)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendGiftCardChangedMsg(giftCard: GiftCard)(implicit user: UserContext) = {
    val msg = GiftCardChanged(user.merchantId, giftCard)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendGiftCardPassChangedMsg(giftCardPass: GiftCardPass)(implicit user: UserContext) = {
    val msg = GiftCardPassChanged(giftCardPass)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendPrepareGiftCardPassReceiptRequestedMsg(entity: GiftCardPass)(implicit user: UserContext): Unit = {
    val msg = PrepareGiftCardPassReceiptRequested(entity)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendGiftCardPassReceiptMsg(
      entity: GiftCardPass,
      email: String,
      merchant: Merchant,
      locationReceipt: LocationReceipt,
      barcodeUrl: String,
      location: Option[Location],
      locationSettings: LocationSettings,
    ): Unit = {
    val msg =
      GiftCardPassReceiptRequested(entity, email, merchant, locationReceipt, barcodeUrl, location, locationSettings)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendLoyaltyProgramChangedMsg(loyaltyProgram: LoyaltyProgram)(implicit user: UserContext) = {
    val msg = LoyaltyProgramChanged(user.merchantId, loyaltyProgram)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendLoyaltyMembershipChanged(loyaltyMembership: LoyaltyMembership)(implicit merchant: MerchantContext): Unit = {
    val msg = LoyaltyMembershipChanged(loyaltyMembership)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendOrderItemUpdatedMsg(entity: OrderItem, locationId: UUID)(implicit user: UserContext): Unit = {
    val msg = OrderItemUpdated(entity, locationId)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendOrderReceiptMsg(
      entity: Order,
      paymentTransactionId: Option[UUID],
      email: String,
      merchant: Merchant,
      locationEmailReceipt: LocationEmailReceipt,
      locationReceipt: LocationReceipt,
      loyaltyMembership: Option[LoyaltyMembership],
      loyaltyProgram: Option[LoyaltyProgram],
    ): Unit = {
    val messages = Seq(
      OrderReceiptRequestedV2(
        entity,
        paymentTransactionId,
        email,
        merchant,
        locationReceipt,
        loyaltyMembership,
        loyaltyProgram,
      ),
    )
    messages.foreach(msg => messageSender ! SendMsgWithRetry(msg))
  }

  def sendOrderCreatedMsg(entity: Order)(implicit user: UserContext): Unit = {
    val msg = OrderChanged.created(entity)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendOrderUpdatedMsg(entity: Order)(implicit user: UserContext): Unit = {
    val msg = OrderChanged.updated(entity)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendOnlineOrderCanceledMsg(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): Unit = {
    val msg = OnlineOrderCanceled(receiptContext)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendOnlineOrderReadyForPickupMsg(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): Unit = {
    val msg = OnlineOrderReadyForPickup(receiptContext)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendOnlineOrderCreatedMsg(receiptContext: ReceiptContext)(implicit merchant: MerchantContext): Unit = {
    val msg = OnlineOrderCreated(receiptContext)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendTicketCreatedMsg(entity: Ticket)(implicit user: UserContext): Unit =
    messageSender ! SendMsgWithRetry(TicketCreatedV2(entity))

  def sendTicketUpdatedMsg(entity: Ticket)(implicit user: UserContext): Unit =
    messageSender ! SendMsgWithRetry(TicketUpdatedV2(entity))

  def sendLocationSettingsUpdatedMsg(locationId: UUID)(implicit user: UserContext): Unit =
    messageSender ! SendMsgWithRetry(LocationSettingsUpdated(locationId))

  def sendOrderSyncedMsg(entity: Order)(implicit user: UserContext): Unit = {
    val msg = OrderSynced(entity)
    messageSender ! SendMsgWithRetry(msg)
  }

  def prepareLoyaltyMembershipSignedUp(
      loyaltyMembership: LoyaltyMembership,
      loyaltyProgram: LoyaltyProgram,
    )(implicit
      merchantContext: MerchantContext,
    ): Unit = {
    val msg = PrepareLoyaltyMembershipSignedUp(loyaltyMembership, loyaltyProgram)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendLoyaltyMembershipSignedUp(
      recipientEmail: String,
      loyaltyMembership: LoyaltyMembership,
      merchant: Merchant,
      loyaltyProgram: LoyaltyProgram,
      barcodeUrl: String,
    )(implicit
      merchantContext: MerchantContext,
    ): Unit = {
    val msg = LoyaltyMembershipSignedUp(recipientEmail, loyaltyMembership, merchant, loyaltyProgram, barcodeUrl)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendOrderAcceptedEmail(receiptContext: ReceiptContext)(implicit merchantContext: MerchantContext): Unit = {
    val msg = OrderAcceptedEmail(receiptContext)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendOrderRejectedEmail(receiptContext: ReceiptContext)(implicit merchantContext: MerchantContext): Unit = {
    val msg = OrderRejectedEmail(receiptContext)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendDeliveryOrderAccepted(order: Order)(implicit merchantContext: MerchantContext): Unit = {
    val msg = DeliveryOrderAccepted(order)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendDeliveryOrderRejected(order: Order)(implicit merchantContext: MerchantContext): Unit = {
    val msg = DeliveryOrderRejected(order)
    messageSender ! SendMsgWithRetry(msg)
  }

  def prepareCashDrawerReport(
      cashDrawer: CashDrawer,
      merchant: Merchant,
      targetUsers: Seq[User],
      location: Location,
      locationReceipt: LocationReceipt,
      cashier: UserInfo,
    )(implicit
      user: UserContext,
    ): Unit = {
    val payload =
      PrepareCashDrawerReportPayload(cashDrawer, merchant, targetUsers, location, locationReceipt, cashier)
    val msg = PrepareCashDrawerReport(payload)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendCashDrawerReport(prepareCashDrawerReport: PrepareCashDrawerReport, activitiesFileUrl: String): Unit = {
    val prepareData = prepareCashDrawerReport.payload.data
    val payload = CashDrawerReportPayload(activitiesFileUrl, prepareData)
    prepareData.targetUsers.foreach { targetUser =>
      val msg = CashDrawerReport(targetUser.email, payload)
      messageSender ! SendMsgWithRetry(msg)
    }
  }

  def sendEntitySynced[Entity: MinimizedForPusherEntity](
      entity: Entity,
      locationId: UUID,
    )(implicit
      user: UserContext,
    ): Unit = {
    val minimizedEntity = MinimizedForPusherEntity[Entity].toMinimized(entity)
    val msg = EntitySynced(minimizedEntity, locationId)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendPasswordReset(
      merchantId: UUID,
      tokenEntity: PasswordResetToken,
      userInfo: UserInfo,
      merchant: Merchant,
    ): Unit = {
    val msg = PasswordResetRequested(merchantId, tokenEntity, userInfo, merchant)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendWelcomePasswordReset(
      merchantId: UUID,
      tokenEntity: PasswordResetToken,
      userInfo: UserInfo,
      merchant: Merchant,
    ): Unit = {
    val msg = WelcomePasswordResetRequested(merchantId, tokenEntity, userInfo, merchant)
    messageSender ! SendMsgWithRetry(msg)
  }

  def sendMerchantChanged(merchant: Merchant, fullPaymentPaymentProcessorConfig: model.PaymentProcessorConfig): Unit =
    messageSender ! SendMsgWithRetry(MerchantChanged(merchant, fullPaymentPaymentProcessorConfig))
}
