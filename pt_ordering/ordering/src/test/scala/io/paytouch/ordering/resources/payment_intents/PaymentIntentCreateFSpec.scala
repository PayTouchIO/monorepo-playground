package io.paytouch.ordering.resources.payment_intents

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ ValidationRejection }
import io.paytouch.ordering.data.model.{ EkashuConfig, JetdirectConfig, WorldpayConfig, WorldpayPaymentType }
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums._
import io.paytouch.ordering.entities.worldpay.WorldpayPaymentStatus
import io.paytouch.ordering.utils.{ FixtureDaoFactory => Factory, Generators }

@scala.annotation.nowarn("msg=Auto-application")
class PaymentIntentCreateFSpec extends PaymentIntentsFSpec {
  abstract class PaymentIntentCreateFSpecContext extends PaymentIntentsFSpecContext {
    val id = UUID.randomUUID
  }

  "POST /v1/payment_intents.create?payment_intent_id=???" in {
    "payment method = Cash" in {
      abstract class Context extends PaymentIntentCreateFSpecContext {
        val creation =
          random[PaymentIntentCreation]
            .copy(
              paymentMethodType = PaymentMethodType.Cash,
              merchantId = merchant.id,
              orderId = order.id,
              orderItemIds = Seq(item1.id),
            )
      }

      "reject the request" in new Context {
        Post(s"/v1/payment_intents.create?payment_intent_id=$id", creation) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
          assertErrorCode("UnsupportedPaymentMethod")
        }
      }
    }

    "payment method = Ekashu" in {
      abstract class Context extends PaymentIntentCreateFSpecContext {
        override lazy val merchant = Factory
          .merchant(
            paymentProcessor = Some(PaymentProcessor.Ekashu),
            paymentProcessorConfig = Some(
              EkashuConfig(
                sellerId = genString.instance,
                sellerKey = genString.instance,
                hashKey = genString.instance,
              ),
            ),
          )
          .create

        val creation = random[PaymentIntentCreation].copy(
          paymentMethodType = PaymentMethodType.Ekashu,
          merchantId = merchant.id,
          orderId = order.id,
        )
      }

      "reject the request" in new Context {
        Post(s"/v1/payment_intents.create?payment_intent_id=$id", creation) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
          assertErrorCode("UnsupportedPaymentMethod")
        }
      }
    }

    "payment method = Jetdirect" in {
      abstract class Context extends PaymentIntentCreateFSpecContext {
        override lazy val merchant = Factory
          .merchant(
            paymentProcessor = Some(PaymentProcessor.Jetdirect),
            paymentProcessorConfig = Some(
              JetdirectConfig(
                merchantId = genString.instance,
                terminalId = genString.instance,
                key = genString.instance,
                securityToken = genString.instance,
              ),
            ),
          )
          .create

        val creation = random[PaymentIntentCreation].copy(
          paymentMethodType = PaymentMethodType.Jetdirect,
          merchantId = merchant.id,
          orderId = order.id,
          orderItemIds = Seq(item1.id),
        )
      }

      "reject the request" in new Context {
        Post(s"/v1/payment_intents.create?payment_intent_id=$id", creation) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
          assertErrorCode("UnsupportedPaymentMethod")
        }
      }
    }

    "payment method = Worldpay" in {
      abstract class Context extends PaymentIntentCreateFSpecContext {
        val worldpayPaymentDao = daos.worldpayPaymentDao

        def assertWorldpayPayment(
            transactionSetupId: String,
            status: WorldpayPaymentStatus,
            paymentIntentId: UUID,
          ) = {
          val maybePayment = worldpayPaymentDao.findByTransactionSetupId(transactionSetupId).await
          maybePayment must beSome
          val payment = maybePayment.get

          payment.status ==== status
          payment.objectId ==== paymentIntentId
          payment.objectType ==== WorldpayPaymentType.PaymentIntent
        }

        override lazy val merchant = Factory
          .merchant(
            paymentProcessor = Some(PaymentProcessor.Worldpay),
            paymentProcessorConfig = Some(
              WorldpayConfig(
                accountId = genString.instance,
                terminalId = genString.instance,
                acceptorId = genString.instance,
                accountToken = genString.instance,
              ),
            ),
          )
          .create

        val creation = random[PaymentIntentCreation].copy(
          paymentMethodType = PaymentMethodType.Worldpay,
          merchantId = merchant.id,
          orderId = order.id,
          orderItemIds = Seq(item1.id, item2.id),
        )
      }

      "creates the intent" in new Context {
        Post(s"/v1/payment_intents.create?payment_intent_id=$id", creation) ~> routes ~> check {
          assertStatus(StatusCodes.Created)

          val entity = responseAs[ApiResponse[PaymentIntent]].data
          entity.paymentMethodType ==== PaymentMethodType.Worldpay

          assertCreation(id, creation)
          assertCalculations(order, creation.tipAmount, entity)
        }
      }

      "creates the worldpay payment" in new Context {
        Post(s"/v1/payment_intents.create?payment_intent_id=$id", creation) ~> routes ~> check {
          assertStatus(StatusCodes.Created)

          val entity = responseAs[ApiResponse[PaymentIntent]].data
          entity.paymentProcessorData must beSome

          val paymentProcessorData = entity.paymentProcessorData.get
          paymentProcessorData.transactionSetupId must beSome
          val transactionSetupId = paymentProcessorData.transactionSetupId.get

          paymentProcessorData.checkoutUrl ==== Some(
            s"http://checkout.worldpay?TransactionSetupId=$transactionSetupId",
          )

          assertWorldpayPayment(transactionSetupId, status = WorldpayPaymentStatus.Submitted, paymentIntentId = id)
        }
      }

      // For v1 only. To be removed for payment intents v2.
      "when not all items are specified" should {
        "reject the request" in new Context {
          val completedCreation = creation.copy(
            orderId = order.id,
            orderItemIds = Seq(item1.id),
          )

          Post(s"/v1/payment_intents.create?payment_intent_id=$id", completedCreation) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("NotIncludedOrderItems")
          }
        }
      }

      "for completed orders" should {
        "reject the request" in new Context {
          val completedCreation = creation.copy(
            orderId = completedOrder.id,
            orderItemIds = Seq(completedItem1.id),
          )

          Post(s"/v1/payment_intents.create?payment_intent_id=$id", completedCreation) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("AlreadyPaidOrderItems")
          }
        }
      }

      "for refunded orders" should {
        "reject the request" in new Context {
          val completedCreation = creation.copy(
            orderId = refundedOrder.id,
            orderItemIds = Seq(refundedItem1.id),
          )

          Post(s"/v1/payment_intents.create?payment_intent_id=$id", completedCreation) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("AlreadyPaidOrderItems")
          }
        }
      }

      "for unknown orders" should {
        "reject the request" in new Context {
          val missingCreation = creation.copy(
            orderId = UUID.randomUUID,
          )

          Post(s"/v1/payment_intents.create?payment_intent_id=$id", missingCreation) ~> routes ~> check {
            rejection ==== ValidationRejection("Order does not exist")
          }
        }
      }

      "with a negative tip" should {
        "replaces the tip with 0" in new Context {
          val negativeTipCreation = creation.copy(tipAmount = Some(BigDecimal(-10)))

          Post(s"/v1/payment_intents.create?payment_intent_id=$id", negativeTipCreation) ~> routes ~> check {
            assertStatus(StatusCodes.Created)

            val entity = responseAs[ApiResponse[PaymentIntent]].data
            entity.tip.amount ==== BigDecimal(0)

            assertCalculations(order, Some(BigDecimal(0)), entity)
          }
        }
      }

      "for unknown order item ids" should {
        "reject the request" in new Context {
          val unknownCreation = creation.copy(
            orderItemIds = Seq(UUID.randomUUID),
          )

          Post(s"/v1/payment_intents.create?payment_intent_id=$id", unknownCreation) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertErrorCode("InvalidOrderItemIds")
          }
        }
      }
    }
  }
}
