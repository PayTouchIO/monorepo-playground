package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import com.softwaremill.macwire._

import org.mockito.Matchers

import io.paytouch.implicits._

import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.data.model.OnlineOrderAttributeRecord
import io.paytouch.core.entities._
import io.paytouch.core.expansions.{ GiftCardPassExpansions, OrderExpansions }
import io.paytouch.core.messages.entities._
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.utils.{ PaytouchLogger, FixtureDaoFactory => Factory }
import io.paytouch.core.utils.PaytouchLogger
import io.paytouch.core.utils.UtcTime
import io.paytouch.core.validators.OrderValidator
import io.paytouch.utils.Tagging._

@scala.annotation.nowarn("msg=Auto-application")
final class OrderSyncServiceSpec extends ServiceDaoSpec {
  abstract class OrderServiceSpecContext extends ServiceDaoSpecContext {
    val messageHandler =
      new SQSMessageHandler(
        actorSystem,
        actorMock.ref.taggedWith[SQSMessageSender],
      )

    override val merchantService = mock[MerchantService]

    override val giftCardPassService: GiftCardPassService =
      wire[GiftCardPassService]

    implicit val logger = new PaytouchLogger

    val service: OrderSyncService = wire[OrderSyncService]

    def getOrderEntity(id: UUID): Order =
      orderService
        .findOpenById(id, orderService.defaultFilters)(OrderExpansions.withFullOrderItems)
        .await
        .get

    val globalCustomer =
      Factory
        .globalCustomerWithEmail(
          merchant = merchant.some,
          email = randomEmail.some,
        )
        .create

    val upsertion =
      randomOrderUpsertion()
        .copy(
          creatorUserId = user.id.some,
          customerId = globalCustomer.id.some,
          locationId = london.id,
          source = Source.Register.some,
          assignedUserIds = None,
          items = Seq.empty,
          deliveryAddress = None,
          onlineOrderAttribute = None,
          bundles = Seq.empty,
        )

    lazy val onlineOrderAttributeUpsertion =
      randomOnlineOrderAttributeUpsertion()

    merchantService.prepareReceiptContext(any, any)(any) answers { args: Array[AnyRef] =>
      random[ReceiptContext]
        .copy(
          recipientEmail = args(0).asInstanceOf[String],
          order = args(1).asInstanceOf[Order],
        )
        .some
        .pure[Future]
    }

    val newOrderId = UUID.randomUUID
  }

  "OrderSyncService" in {
    "syncById" should {
      "if it creates a new order" should {
        "send order_created and order_synced messages" in new OrderServiceSpecContext {
          service.syncById(newOrderId, upsertion)(userCtx).await.success

          val orderEntity = getOrderEntity(newOrderId)

          actorMock.expectMsg(SendMsgWithRetry(OrderChanged.created(orderEntity)))
          actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
        }

        "if source=storefront" in {
          "not send any SQS messages for orders whose acceptance status is open" in new OrderServiceSpecContext {
            val storefrontUpsertion = upsertion.copy(
              source = Some(Source.Storefront),
              status = OrderStatus.Received,
              onlineOrderAttribute =
                Some(onlineOrderAttributeUpsertion.copy(acceptanceStatus = Some(AcceptanceStatus.Open))),
            )
            service.syncById(newOrderId, storefrontUpsertion)(userCtx).await.success

            val orderEntity = getOrderEntity(newOrderId)

            actorMock.expectNoMessage()
          }

          "send order_created, online_order_created, order_synced messages" in new OrderServiceSpecContext {
            val storefrontUpsertion = upsertion.copy(
              source = Some(Source.Storefront),
              status = OrderStatus.Received,
              onlineOrderAttribute =
                Some(onlineOrderAttributeUpsertion.copy(acceptanceStatus = Some(AcceptanceStatus.Pending))),
            )

            service.syncById(newOrderId, storefrontUpsertion)(userCtx).await.success

            val orderEntity = getOrderEntity(newOrderId)

            actorMock.expectMsg(SendMsgWithRetry(OrderChanged.created(orderEntity)))
            actorMock.expectMsgPF(hint = "OnlineOrderCreated") { // can't match the exact match because status transations have random UUIDs
              case t @ SendMsgWithRetry(OnlineOrderCreated(_, o)) if o.data.id == orderEntity.id => true
            }
            actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
          }

          "if status=canceled" in {
            "not send any SQS messages if acceptance status = open" in new OrderServiceSpecContext {
              val storefrontUpsertion = upsertion.copy(
                source = Some(Source.Storefront),
                status = OrderStatus.Canceled,
                onlineOrderAttribute =
                  Some(onlineOrderAttributeUpsertion.copy(acceptanceStatus = Some(AcceptanceStatus.Open))),
              )

              service.syncById(newOrderId, storefrontUpsertion)(userCtx).await.success

              val orderEntity = getOrderEntity(newOrderId)

              actorMock.expectNoMessage()
            }

            "send order_created, online_order_created, online_order_canceled, order_synced messages" in new OrderServiceSpecContext {
              val storefrontUpsertion = upsertion.copy(
                source = Some(Source.Storefront),
                status = OrderStatus.Canceled,
                onlineOrderAttribute =
                  Some(onlineOrderAttributeUpsertion.copy(acceptanceStatus = Some(AcceptanceStatus.Pending))),
              )

              service.syncById(newOrderId, storefrontUpsertion)(userCtx).await.success

              val orderEntity = getOrderEntity(newOrderId)

              actorMock.expectMsg(SendMsgWithRetry(OrderChanged.created(orderEntity)))
              actorMock.expectMsgPF(hint = "OnlineOrderCreated") { // can't match the exact match because status transations have random UUIDs
                case t @ SendMsgWithRetry(OnlineOrderCreated(_, o)) if o.data.id == orderEntity.id => true
              }
              actorMock.expectMsgPF(hint = "OnlineOrderCanceled") { // can't match the exact match because status transations have random UUIDs
                case t @ SendMsgWithRetry(OnlineOrderCanceled(_, o)) if o.data.id == orderEntity.id => true
              }
              actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
            }
          }
        }

        "if it creates a gift card pass" should {
          "send a gift_card_pass_changed message" in new OrderServiceSpecContext {
            val giftCardProduct = Factory.giftCardProduct(merchant).create
            val giftCard = Factory.giftCard(giftCardProduct).create

            val giftCardUpsertion =
              randomOrderItemUpsertion().copy(
                id = UUID.randomUUID,
                productId = Some(giftCardProduct.id),
                priceAmount = Some(10),
                paymentStatus = Some(PaymentStatus.Paid),
              )

            val upsertionWithGiftCard =
              upsertion.copy(items = Seq(giftCardUpsertion))

            service
              .syncById(newOrderId, upsertionWithGiftCard)(userCtx)
              .await
              .success

            val giftCardPassEntity =
              giftCardPassService
                .findByOrderItemId(giftCardUpsertion.id)(GiftCardPassExpansions(withTransactions = true))(userCtx)
                .await
                .get

            val orderEntity = getOrderEntity(newOrderId)

            actorMock.expectMsg(SendMsgWithRetry(GiftCardPassChanged(giftCardPassEntity)))
            actorMock.expectMsg(SendMsgWithRetry(OrderChanged.created(orderEntity)))
            actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
          }
        }
      }

      "if it updates an existing order" should {
        "send order_updated and order_synced message" in new OrderServiceSpecContext {
          val order = Factory.order(merchant).create

          service.syncById(order.id, upsertion)(userCtx).await.success

          val orderEntity = getOrderEntity(order.id)

          actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(orderEntity)))
          actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
        }

        "source = storefront" should {
          "send online_order_created for orders whose acceptance status transitions to pending" in new OrderServiceSpecContext {
            val onlineOrderAttribute =
              Factory.onlineOrderAttribute(merchant, acceptanceStatus = Some(AcceptanceStatus.Open)).create
            val order = Factory.order(merchant, onlineOrderAttribute = Some(onlineOrderAttribute)).create

            val openUpsertion = upsertion.copy(
              source = Some(Source.Storefront),
              status = OrderStatus.Received,
              onlineOrderAttribute = Some(
                onlineOrderAttributeUpsertion.copy(
                  id = onlineOrderAttribute.id,
                  acceptanceStatus = Some(AcceptanceStatus.Open),
                ),
              ),
            )

            service.syncById(newOrderId, openUpsertion)(userCtx).await.success

            val openEntity = getOrderEntity(newOrderId)

            actorMock.expectNoMessage()

            val pendingUpsertion = upsertion.copy(
              source = Some(Source.Storefront),
              status = OrderStatus.Received,
              onlineOrderAttribute = Some(
                onlineOrderAttributeUpsertion.copy(
                  id = onlineOrderAttribute.id,
                  acceptanceStatus = Some(AcceptanceStatus.Pending),
                ),
              ),
            )
            service.syncById(newOrderId, pendingUpsertion)(userCtx).await.success

            val pendingEntity = getOrderEntity(newOrderId)

            actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(pendingEntity)))
            actorMock.expectMsgPF(hint = "OnlineOrderCreated") { // can't match the exact match because status transations have random UUIDs
              case t @ SendMsgWithRetry(OnlineOrderCreated(_, o)) if o.data.id == pendingEntity.id => true
            }
            actorMock.expectMsg(SendMsgWithRetry(OrderSynced(pendingEntity)))
          }

          "send order_created, online_order_created, online_order_ready_for_pickup, order_synced messages" in new OrderServiceSpecContext {
            val updatedUpsertion = upsertion.copy(
              source = Some(Source.Storefront),
              `type` = OrderType.TakeOut,
              status = OrderStatus.Completed,
              onlineOrderAttribute = onlineOrderAttributeUpsertion
                .copy(
                  acceptanceStatus = AcceptanceStatus.Pending.some,
                )
                .some,
            )

            service.syncById(newOrderId, updatedUpsertion)(userCtx).await.success

            val orderEntity = getOrderEntity(newOrderId)
            orderEntity.`type` ==== OrderType.TakeOut.some
            orderEntity.status ==== OrderStatus.Completed.some

            actorMock.expectMsg(SendMsgWithRetry(OrderChanged.created(orderEntity)))

            actorMock.expectMsgPF(hint = "OnlineOrderCreated") {
              case t @ SendMsgWithRetry(OnlineOrderCreated(_, o)) if o.data.id == orderEntity.id => true
            }

            actorMock.expectMsgPF(hint = "OnlineOrderReadyForPickup") {
              case t @ SendMsgWithRetry(OnlineOrderReadyForPickup(_, o)) if o.data.id == orderEntity.id => true
            }

            actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
          }

          "send order_changed, online_order_created, online_order_ready_for_pickup (if estimatedReadyAt == acceptedAt (ie customer is NOT on site)), order_synced messages" in new OrderServiceSpecContext {
            val now = UtcTime.now

            val onlineOrderAttributeRecord: OnlineOrderAttributeRecord =
              Factory
                .onlineOrderAttribute(merchant)
                .createForceOverride(
                  _.copy(
                    acceptanceStatus = AcceptanceStatus.Open.some,
                    acceptedAt = now.some, // customer is NOT on site
                    estimatedReadyAt = now.plusMinutes(30).some, // customer is NOT on site
                  ),
                )

            val order = Factory
              .order(
                merchant,
                onlineOrderAttribute = onlineOrderAttributeRecord.some,
              )
              .create

            val updatedUpsertion = upsertion.copy(
              source = Some(Source.Storefront),
              `type` = OrderType.TakeOut,
              status = OrderStatus.Completed,
              onlineOrderAttribute = onlineOrderAttributeUpsertion
                .copy(
                  acceptanceStatus = AcceptanceStatus.Pending.some,
                )
                .some,
            )

            service.syncById(order.id, updatedUpsertion)(userCtx).await.success

            val orderEntity = getOrderEntity(order.id)
            orderEntity.`type` ==== OrderType.TakeOut.some
            orderEntity.status ==== OrderStatus.Completed.some

            actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(orderEntity)))

            actorMock.expectMsgPF(hint = "OnlineOrderCreated") {
              case t @ SendMsgWithRetry(OnlineOrderCreated(_, o)) if o.data.id == orderEntity.id => true
            }

            actorMock.expectMsgPF(hint = "OnlineOrderReadyForPickup") {
              case t @ SendMsgWithRetry(OnlineOrderReadyForPickup(_, o)) if o.data.id == orderEntity.id => true
            }

            actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
          }

          "send order_changed, online_order_created, and order_synced messages but NOT online_order_ready_for_pickup if estimatedReadyAt == acceptedAt (ie customer is on site)" in new OrderServiceSpecContext {
            val now = UtcTime.now

            val onlineOrderAttributeRecord: OnlineOrderAttributeRecord =
              Factory
                .onlineOrderAttribute(merchant)
                .createForceOverride(
                  _.copy(
                    acceptanceStatus = AcceptanceStatus.Open.some,
                    estimatedReadyAt = now.some, // customer is on site
                    acceptedAt = now.some, // customer is on site
                  ),
                )

            val order = Factory
              .order(
                merchant,
                onlineOrderAttribute = onlineOrderAttributeRecord.some,
              )
              .create

            val updatedUpsertion = upsertion.copy(
              source = Some(Source.Storefront),
              `type` = OrderType.TakeOut,
              status = OrderStatus.Completed,
              onlineOrderAttribute = onlineOrderAttributeUpsertion
                .copy(
                  acceptanceStatus = AcceptanceStatus.Pending.some,
                )
                .some,
            )

            service.syncById(order.id, updatedUpsertion)(userCtx).await.success

            val orderEntity = getOrderEntity(order.id)
            orderEntity.`type` ==== OrderType.TakeOut.some
            orderEntity.status ==== OrderStatus.Completed.some

            actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(orderEntity)))

            actorMock.expectMsgPF(hint = "OnlineOrderCreated") {
              case t @ SendMsgWithRetry(OnlineOrderCreated(_, o)) if o.data.id == orderEntity.id => true
            }

            // actorMock.expectMsgPF(hint = "OnlineOrderReadyForPickup") {
            //   case t @ SendMsgWithRetry(OnlineOrderReadyForPickup(_, o)) if o.data.id == orderEntity.id => true
            // }

            actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
          }
        }

        "set recipientEmail" in new OrderServiceSpecContext {
          val onlineOrderAttribute =
            Factory
              .onlineOrderAttribute(
                merchant,
                acceptanceStatus = AcceptanceStatus.Pending.some,
              )
              .create

          val order =
            Factory
              .order(
                merchant,
                paymentStatus = PaymentStatus.Pending.some,
                onlineOrderAttribute = onlineOrderAttribute.some,
              )
              .create

          val giftCardProduct =
            Factory
              .giftCardProduct(merchant)
              .create

          val orderItem =
            Factory
              .orderItem(
                order,
                product = giftCardProduct.some,
                paymentStatus = PaymentStatus.Pending.some,
              )
              .create

          Factory
            .giftCard(giftCardProduct)
            .create

          val giftCardUpsertion =
            randomOrderItemUpsertion().copy(
              id = orderItem.id,
              productId = Some(giftCardProduct.id),
              priceAmount = Some(10),
              paymentStatus = Some(PaymentStatus.Paid),
              giftCardPassRecipientEmail = "a@b.com".some,
            )

          val upsertionWithGiftCard =
            upsertion.copy(
              items = Seq(giftCardUpsertion),
              paymentStatus = PaymentStatus.Paid,
            )

          service
            .syncById(order.id, upsertionWithGiftCard)(userCtx)
            .await
            .success

          val giftCardPassEntity =
            giftCardPassService
              .findByOrderItemId(giftCardUpsertion.id)(GiftCardPassExpansions(withTransactions = true))(userCtx)
              .await
              .get

          giftCardPassEntity.recipientEmail.isDefined ==== true
          giftCardPassEntity.recipientEmail ==== giftCardUpsertion.giftCardPassRecipientEmail

          val orderEntity =
            getOrderEntity(order.id)

          actorMock.expectMsg(SendMsgWithRetry(PrepareGiftCardPassReceiptRequested(giftCardPassEntity)))
          actorMock.expectMsg(SendMsgWithRetry(GiftCardPassChanged(giftCardPassEntity)))
          actorMock.expectMsg(SendMsgWithRetry(OrderChanged.updated(orderEntity)))
          actorMock.expectMsg(SendMsgWithRetry(OrderSynced(orderEntity)))
        }
      }
    }
  }
}
