package io.paytouch.core.resources.orders

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

import cats.implicits._

import org.scalacheck._

import io.paytouch.implicits._

import io.paytouch.core._
import io.paytouch.core.data.daos._
import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums._
import io.paytouch.core.errors._
import io.paytouch.core.services.OrderService
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, _ }

import OrdersStorePaymentTransactionFSpec._

class OrdersStorePaymentTransactionFSpec extends OrdersFSpec {
  abstract class OrdersStorePaymentTransactionFSpecContext extends OrderResourceFSpecContext with AppTokenFixtures {
    def randomUpsertion(
        `type`: Option[TransactionType] = None,
        paymentType: Option[TransactionPaymentType] = None,
        paymentDetails: Option[PaymentDetails] = None,
        orderItemIds: Option[Seq[UUID]] = None,
      ): OrderService.PaymentTransactionUpsertion =
      OrderService.PaymentTransactionUpsertion(
        id = UUID.randomUUID,
        `type` = `type`.getOrElse(TransactionType.Payment),
        paymentType = paymentType.getOrElse(randomOnce[TransactionPaymentType]),
        paymentDetails = paymentDetails.getOrElse(
          PaymentDetails(transactionResult = Some(CardTransactionResultType.Approved), amount = None),
        ),
        paidAt = UtcTime.now,
        orderItemIds = orderItemIds,
        version = 1,
        paymentProcessor = genTransactionPaymentProcessor.instance,
      )

    def assertTransactionUpsertion(orderId: UUID, upsertion: OrderService.PaymentTransactionUpsertion) = {
      val transaction =
        daos
          .paymentTransactionDao
          .findById(upsertion.id)
          .await
          .get

      transaction.id ==== upsertion.id
      transaction.orderId ==== orderId
      transaction.`type`.get ==== upsertion.`type`
      transaction.paymentType.get ==== upsertion.paymentType
      transaction.paymentDetails.flatMap(_.amount).get ==== upsertion.paymentDetails.amount.get
      transaction.paidAt.get ==== upsertion.paidAt
      transaction.version ==== upsertion.version
    }

    def assertOrder(
        orderId: UUID,
        status: Option[OrderStatus] = None,
        paymentStatus: Option[PaymentStatus] = None,
        paymentType: Option[OrderPaymentType] = None,
        completedAt: Option[ZonedDateTime] = None,
        tipAmount: Option[BigDecimal] = None,
        acceptanceStatus: Option[AcceptanceStatus] = None,
      ) = {
      val order =
        orderDao
          .findOpenById(orderId)
          .await
          .get

      status.map(order.status ==== Some(_))
      paymentStatus.map(order.paymentStatus ==== Some(_))
      paymentType.map(order.paymentType ==== Some(_))
      completedAt.map(order.completedAt.get should beGreaterThanOrEqualTo(_))
      tipAmount.map(order.tipAmount ==== Some(_))

      acceptanceStatus.map(onlineOrderAttributeDao.findByOrderId(orderId).await.get.acceptanceStatus ==== _)
    }
  }

  "POST /v1/orders.store_payment_transaction?order_id=$" in {
    "if request has valid token" in {
      "when transaction amount == order amount" should {
        "mark the order as paid AND sends gift card pass receipts AND doesn't delete data" in new OrdersStorePaymentTransactionFSpecContext {
          // DEFINITIONS
          val product = Factory.simpleProduct(merchant).create
          val giftCardProduct = Factory.giftCardProduct(merchant).create
          val giftCard = Factory.giftCard(giftCardProduct).create

          val discount1 = Factory.discount(merchant).create
          val discount2 = Factory.discount(merchant).create

          val modifierSet = Factory.modifierSet(merchant).create
          val modifierOption1 = Factory.modifierOption(modifierSet).create
          val modifierOption2 = Factory.modifierOption(modifierSet).create

          val taxRate = Factory.taxRate(merchant).create

          val template = Factory.templateProduct(merchant).create
          val variantOptionType = Factory.variantOptionType(template).create
          val variantOption1 = Factory.variantOption(template, variantOptionType, "foo").create
          val variantOption2 = Factory.variantOption(template, variantOptionType, "foo").create

          val bundle = Factory.comboProduct(merchant).create
          val bundleSet = Factory.bundleSet(bundle).create
          val bundleOption = Factory.bundleOption(bundleSet, product).create

          // EXISTING ORDER DATA
          val order =
            Factory
              .order(
                merchant,
                location = Some(london),
                subtotalAmount = Some(1.0),
                taxAmount = Some(0.2),
                totalAmount = Some(1.2),
                deliveryFeeAmount = Some(0),
                status = Some(OrderStatus.InProgress),
                paymentStatus = Some(PaymentStatus.Pending),
              )
              .create
          val orderItemGiftCard = Factory
            .orderItem(
              order,
              product = giftCardProduct.some,
              giftCardPassRecipientEmail = "foo@example.com".some,
            )
            .create
          val orderItemProduct = Factory
            .orderItem(
              order,
              product = giftCardProduct.some,
            )
            .create
          val orderItemDiscount1 = Factory.orderItemDiscount(orderItemProduct, discount1).create
          val orderItemDiscount2 = Factory.orderItemDiscount(orderItemProduct).create
          val orderItemDiscount3 = Factory.orderItemDiscount(orderItemProduct).create
          val orderItemModifierOption = Factory.orderItemModifierOption(orderItemProduct, modifierOption1).create
          val orderItemVariantOption = Factory.orderItemVariantOption(orderItemProduct, variantOption1).create
          val orderItemTaxRate = Factory.orderItemTaxRate(orderItemProduct, taxRate).create

          val paymentTransaction = Factory.paymentTransaction(order).create
          val paymentTransactionOrderItem =
            Factory.paymentTransactionOrderItem(paymentTransaction, orderItemProduct).create
          val orderDiscount = Factory.orderDiscount(order, discount2).create

          val orderBundle =
            Factory.orderBundle(order, orderItemProduct, orderItemProduct, Some(bundleSet), Some(bundleOption)).create

          val transaction = randomUpsertion(
            paymentDetails = Some(
              PaymentDetails(
                transactionResult = CardTransactionResultType.Approved.some,
                amount = Some(1.2),
              ),
            ),
          )

          giftCardPassDao.findByOrderItemId(orderItemGiftCard.id).await ==== None

          Post(s"/v1/orders.store_payment_transaction?order_id=${order.id}", transaction)
            .addHeader(appAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderEntity = responseAs[ApiResponse[entities.Order]].data
            orderEntity.id ==== order.id
            assertTransactionUpsertion(order.id, transaction)
            assertOrder(
              order.id,
              status = Some(OrderStatus.InProgress),
              paymentStatus = Some(PaymentStatus.Paid),
              paymentType = Some(transaction.paymentType.toOrderPaymentType),
            )

            val reloadedOrderItem = orderItemDao.findById(orderItemGiftCard.id).await.get
            reloadedOrderItem.paymentStatus ==== PaymentStatus.Paid.some
            val giftCardPass = giftCardPassDao.findByOrderItemId(orderItemGiftCard.id).await.get
            // as a side-effect of sending receipt, gift card pass gets the recipient email from the order item
            orderItemGiftCard.giftCardPassRecipientEmail ==== giftCardPass.recipientEmail

            daos.orderDiscountDao.findById(orderDiscount.id).await must beSome
            daos.orderItemDiscountDao.findById(orderItemDiscount1.id).await must beSome
            daos.orderItemModifierOptionDao.findById(orderItemModifierOption.id).await must beSome
            daos.orderItemTaxRateDao.findById(orderItemTaxRate.id).await must beSome
            daos.orderItemVariantOptionDao.findById(orderItemVariantOption.id).await must beSome
          }
        }
        "mark the online order as paid even if the order is open" in new OrdersStorePaymentTransactionFSpecContext {
          val transaction = randomUpsertion(
            paymentDetails = Some(
              PaymentDetails(
                transactionResult = CardTransactionResultType.Approved.some,
                amount = Some(1.2),
              ),
            ),
          )

          val order =
            Factory
              .order(
                merchant,
                subtotalAmount = Some(1.0),
                taxAmount = Some(0.2),
                totalAmount = Some(1.2),
                deliveryFeeAmount = Some(0),
                status = Some(OrderStatus.InProgress),
                paymentStatus = Some(PaymentStatus.Pending),
                onlineOrderAttribute = Some(Factory.onlineOrderAttribute(merchant, Some(AcceptanceStatus.Open)).create),
              )
              .create

          Post(s"/v1/orders.store_payment_transaction?order_id=${order.id}", transaction)
            .addHeader(appAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderEntity = responseAs[ApiResponse[entities.Order]].data
            orderEntity.id ==== order.id
            assertTransactionUpsertion(order.id, transaction)
            assertOrder(
              order.id,
              status = Some(OrderStatus.InProgress),
              paymentStatus = Some(PaymentStatus.Paid),
              paymentType = Some(transaction.paymentType.toOrderPaymentType),
              acceptanceStatus = Some(AcceptanceStatus.Pending),
            )
          }
        }

        "mark the order as completed when autocomplete = true" in new OrdersStorePaymentTransactionFSpecContext {
          val transaction = randomUpsertion(
            paymentDetails = Some(
              PaymentDetails(
                transactionResult = CardTransactionResultType.Approved.some,
                amount = Some(1.2),
              ),
            ),
          )

          val order =
            Factory
              .order(merchant, Some(london), totalAmount = Some(1.2), status = Some(OrderStatus.InProgress))
              .create

          val kitchen = Factory.kitchen(london).create

          val ticket =
            Factory
              .ticket(
                order,
                status = Some(TicketStatus.Completed),
                routeToKitchenId = kitchen.id,
              )
              .create

          Factory
            .locationSettings(
              london,
              orderAutocomplete = Some(true),
            )
            .create

          Post(s"/v1/orders.store_payment_transaction?order_id=${order.id}", transaction)
            .addHeader(appAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderEntity = responseAs[ApiResponse[entities.Order]].data
            orderEntity.id ==== order.id
            assertTransactionUpsertion(order.id, transaction)

            assertOrder(
              order.id,
              status = Some(OrderStatus.Completed),
              paymentStatus = Some(PaymentStatus.Paid),
              paymentType = Some(transaction.paymentType.toOrderPaymentType),
              completedAt = Some(transaction.paidAt),
            )
          }

          Post(s"/v1/orders.store_payment_transaction?order_id=${order.id}", transaction)
            .addHeader(appAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderEntity = responseAs[ApiResponse[entities.Order]].data
            orderEntity.id ==== order.id
            assertTransactionUpsertion(order.id, transaction)

          }
        }
      }

      "payment transaction with tip" should {
        "mark the order as paid and store the tip" in new OrdersStorePaymentTransactionFSpecContext {
          val transaction = randomUpsertion(
            paymentDetails = Some(
              PaymentDetails(
                transactionResult = CardTransactionResultType.Approved.some,
                amount = Some(2.2),
                tipAmount = 1.0,
              ),
            ),
          )

          val order =
            Factory
              .order(merchant, totalAmount = Some(1.2), tipAmount = Some(0.0), status = Some(OrderStatus.InProgress))
              .create

          Post(s"/v1/orders.store_payment_transaction?order_id=${order.id}", transaction)
            .addHeader(appAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val orderEntity = responseAs[ApiResponse[entities.Order]].data
            orderEntity.id ==== order.id
            assertTransactionUpsertion(order.id, transaction)
            assertOrder(
              order.id,
              status = Some(OrderStatus.InProgress),
              paymentStatus = Some(PaymentStatus.Paid),
              paymentType = Some(transaction.paymentType.toOrderPaymentType),
              tipAmount = Some(1.0),
            )
          }
        }
      }

      "append transactions" in new OrdersStorePaymentTransactionFSpecContext {
        val transaction = randomUpsertion(
          paymentDetails = Some(
            PaymentDetails(
              transactionResult = CardTransactionResultType.Approved.some,
              amount = Some(1.2),
            ),
          ),
        )

        val order =
          Factory
            .order(merchant, totalAmount = Some(1.2))
            .create

        Post(s"/v1/orders.store_payment_transaction?order_id=${order.id}", transaction)
          .addHeader(appAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()
          val orderEntity = responseAs[ApiResponse[entities.Order]].data
          orderEntity.id ==== order.id
          assertTransactionUpsertion(order.id, transaction)
        }

        // transactions should be appended instead of overwritten
        val transaction2 = transaction.copy(id = UUID.randomUUID)
        Post(
          s"/v1/orders.store_payment_transaction?order_id=${order.id}",
          transaction2,
        ).addHeader(appAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()
          val orderEntity = responseAs[ApiResponse[entities.Order]].data
          orderEntity.id ==== order.id
          assertTransactionUpsertion(order.id, transaction2)

          daos
            .paymentTransactionDao
            .findByOrderIds(Seq(order.id))
            .map(_.size)
            .await ==== 2
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new OrdersStorePaymentTransactionFSpecContext {
        val orderId = randomOnce[UUID]
        val transaction = randomUpsertion()

        Post(
          s"/v1/orders.store_payment_transaction?order_id=${orderId}",
          transaction,
        ).addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}

object OrdersStorePaymentTransactionFSpec {
  private def randomNotPayment: TransactionType =
    Gen
      .oneOf(TransactionType.values)
      .filterNot(_ == TransactionType.Payment)
      .sample
      .getOrElse(TransactionType.Void)

  private def randomNotApprovedOption: Option[CardTransactionResultType] =
    Gen
      .oneOf(CardTransactionResultType.values)
      .filterNot(_ == CardTransactionResultType.Approved)
      .sample
}
