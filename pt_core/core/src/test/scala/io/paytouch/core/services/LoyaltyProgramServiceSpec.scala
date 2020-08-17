package io.paytouch.core.services

import java.util.UUID

import com.softwaremill.macwire._
import io.paytouch.core.async.sqs.{ SQSMessageSender, SendMsgWithRetry }
import io.paytouch.core.data.model.enums.{ PaymentStatus, TransactionPaymentType, TransactionType }
import io.paytouch.core.data.model.{ LoyaltyMembershipRecord, LoyaltyProgramRecord }
import io.paytouch.core.entities.enums.LoyaltyProgramType
import io.paytouch.core.entities.{ LoyaltyProgramCreation, LoyaltyProgramUpdate, PaymentDetails }
import io.paytouch.core.messages.SQSMessageHandler
import io.paytouch.core.messages.entities.LoyaltyProgramChanged
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class LoyaltyProgramServiceSpec extends ServiceDaoSpec {

  abstract class LoyaltyProgramServiceSpecContext extends ServiceDaoSpecContext { self =>
    val messageHandler = new SQSMessageHandler(actorSystem, actorMock.ref.taggedWith[SQSMessageSender])

    val loyaltyMembershipDao = daos.loyaltyMembershipDao

    val service = wire[LoyaltyProgramService]
    val globalCustomer = Factory.globalCustomer().create
    val customer = Factory.customerMerchant(merchant, globalCustomer).create
    val order = Factory.order(merchant, Some(rome), Some(customer), totalAmount = Some(40), tipAmount = Some(0)).create
    Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid), totalPriceAmount = Some(40)).create
    Factory
      .paymentTransaction(
        order,
        paymentType = Some(TransactionPaymentType.Cash),
        paymentDetails = Some(PaymentDetails(amount = Some(40))),
        `type` = Some(TransactionType.Payment),
      )
      .create

    def assertNothingHappens(customerId: UUID) =
      assertCustomerIsNotEnrolledInAnyLoyaltyProgram(customerId)

    def assertCustomerIsNotEnrolledInAnyLoyaltyProgram(customerId: UUID) = {
      val result = loyaltyMembershipDao.findByCustomerId(merchant.id, customerId).await
      result should beEmpty
    }

    def assertCustomerIsLinked(customerId: UUID, loyaltyProgram: LoyaltyProgramRecord) = {
      val maybeMembership =
        loyaltyMembershipDao.findByCustomerIdAndLoyaltyProgramId(merchant.id, customerId, loyaltyProgram.id).await
      maybeMembership should beSome
      maybeMembership.get
    }

    def assertCustomerIsEnrolled(customerId: UUID, loyaltyProgram: LoyaltyProgramRecord) = {
      val loyaltyMembership = assertCustomerIsLinked(customerId, loyaltyProgram)
      loyaltyMembership.isEnrolled must beTrue
    }

    def assertOrderPointsAssigned(
        customerId: UUID,
        loyaltyProgram: LoyaltyProgramRecord,
        points: Int,
      ) = {
      val result =
        loyaltyMembershipDao.findByCustomerIdAndLoyaltyProgramId(merchant.id, customerId, loyaltyProgram.id).await.get
      result.points ==== points
    }

    def assertOrderPointsNotAssigned(loyaltyMembership: LoyaltyMembershipRecord) = {
      val result =
        loyaltyMembershipDao.findById(loyaltyMembership.id).await.get
      result.points ==== 0
    }

  }

  "LoyaltyProgramService" in {
    "upsert" should {
      "if loyalty program was created" should {
        "send a LoyaltyProgramChange message" in new LoyaltyProgramServiceSpecContext {
          val randomUuid = UUID.randomUUID
          val upsertion = random[LoyaltyProgramCreation].copy(rewards = None)
          val (resultType, entity) = service.create(randomUuid, upsertion).await.success

          actorMock.expectMsg(SendMsgWithRetry(LoyaltyProgramChanged(merchant.id, entity)))
        }
      }
      "if loyalty program was updated" should {
        "send a LoyaltyProgramChange message" in new LoyaltyProgramServiceSpecContext {
          val loyaltyProgram = Factory.loyaltyProgram(merchant).create
          val upsertion =
            random[LoyaltyProgramUpdate].copy(
              points = Some(9),
              `type` = Some(genLoyaltyProgramType.instance),
              rewards = None,
            )
          val (resultType, entity) = service.update(loyaltyProgram.id, upsertion).await.success

          actorMock.expectMsg(SendMsgWithRetry(LoyaltyProgramChanged(merchant.id, entity)))
        }
      }
    }

    "logPoints" should {
      "if merchant doesn't have a loyalty program" should {
        "do nothing" in new LoyaltyProgramServiceSpecContext {
          service.logPoints(order).await

          assertNothingHappens(order.customerId.get)
        }
      }
      "if merchant has an active loyalty program but it's not active in the order's location" should {
        "do nothing" in new LoyaltyProgramServiceSpecContext {
          val loyaltyProgram = Factory.loyaltyProgram(merchant).create
          service.logPoints(order).await

          assertNothingHappens(order.customerId.get)
        }
      }
      "if merchant has a disabled loyalty program and it's not active in the order's location" should {
        "do nothing" in new LoyaltyProgramServiceSpecContext {
          val loyaltyProgram = Factory.loyaltyProgram(merchant, active = Some(false), locations = Seq(rome)).create
          service.logPoints(order).await

          assertNothingHappens(order.customerId.get)
        }
      }
      "if merchant has a valid and active loyalty program in the order's location" should {

        "if customer id is None" should {
          "not explode" in new LoyaltyProgramServiceSpecContext {
            val loyaltyProgram = Factory.loyaltyProgram(merchant, locations = Seq(rome)).create
            val orderWithNoCustomer = order.copy(customerId = None)
            service.logPoints(orderWithNoCustomer).await
          }
        }

        "if customer is not enrolled in the loyalty program" should {
          "do nothing" in new LoyaltyProgramServiceSpecContext {
            val loyaltyProgram = Factory.loyaltyProgram(merchant, locations = Seq(rome)).create
            val customerId = order.customerId.get

            service.logPoints(order).await

            assertNothingHappens(order.customerId.get)
          }
        }

        "if customer is linked to the loyalty program but not enrolled" should {
          "do not assign points" in new LoyaltyProgramServiceSpecContext {
            val loyaltyProgram = Factory.loyaltyProgram(merchant, locations = Seq(rome)).create
            val loyaltyMembership = Factory.loyaltyMembership(globalCustomer, loyaltyProgram, points = Some(0)).create

            service.logPoints(order).await

            assertCustomerIsLinked(customer.id, loyaltyProgram)
            assertOrderPointsNotAssigned(loyaltyMembership)
          }
        }

        "if customer is enrolled in the loyalty program" should {
          "log new earned points" in new LoyaltyProgramServiceSpecContext {
            val loyaltyProgram = Factory
              .loyaltyProgram(
                merchant,
                locations = Seq(rome),
                `type` = Some(LoyaltyProgramType.Spend),
                points = Some(100),
                minimumPurchaseAmount = Some(10),
                spendAmountForPoints = Some(5),
              )
              .create
            Factory.loyaltyMembership(globalCustomer, loyaltyProgram, merchantOptInAt = Some(UtcTime.now)).create

            service.logPoints(order).await

            assertCustomerIsEnrolled(customer.id, loyaltyProgram)
            assertOrderPointsAssigned(customer.id, loyaltyProgram, 800)
          }
        }

        "if it is called more than once" should {
          "be idempotent" in new LoyaltyProgramServiceSpecContext {
            val loyaltyProgram = Factory
              .loyaltyProgram(
                merchant,
                locations = Seq(rome),
                `type` = Some(LoyaltyProgramType.Spend),
                points = Some(100),
                minimumPurchaseAmount = Some(10),
                spendAmountForPoints = Some(5),
              )
              .create
            Factory.loyaltyMembership(globalCustomer, loyaltyProgram, merchantOptInAt = Some(UtcTime.now)).create

            service.logPoints(order).await
            service.logPoints(order).await

            assertCustomerIsLinked(customer.id, loyaltyProgram)
            assertOrderPointsAssigned(customer.id, loyaltyProgram, 800)
          }
        }

        "if it is called on different orders" should {
          "sum points from different orders" in new LoyaltyProgramServiceSpecContext {
            val otherOrder = Factory
              .order(
                merchant,
                Some(rome),
                Some(customer),
                totalAmount = Some(20),
                tipAmount = Some(0),
                paymentStatus = Some(PaymentStatus.PartiallyRefunded),
              )
              .create
            Factory.orderItem(otherOrder, paymentStatus = Some(PaymentStatus.Paid), totalPriceAmount = Some(20)).create
            Factory
              .orderItem(otherOrder, paymentStatus = Some(PaymentStatus.Refunded), totalPriceAmount = Some(10))
              .create
            Factory
              .paymentTransaction(
                otherOrder,
                paymentType = Some(TransactionPaymentType.Cash),
                paymentDetails = Some(PaymentDetails(amount = Some(20))),
                `type` = Some(TransactionType.Payment),
              )
              .create
            Factory
              .paymentTransaction(
                otherOrder,
                paymentType = Some(TransactionPaymentType.Cash),
                paymentDetails = Some(PaymentDetails(amount = Some(10))),
                `type` = Some(TransactionType.Refund),
              )
              .create
            val loyaltyProgram = Factory
              .loyaltyProgram(
                merchant,
                locations = Seq(rome),
                `type` = Some(LoyaltyProgramType.Spend),
                points = Some(100),
                minimumPurchaseAmount = Some(10),
                spendAmountForPoints = Some(5),
              )
              .create
            val loyaltyMembership =
              Factory.loyaltyMembership(globalCustomer, loyaltyProgram, merchantOptInAt = Some(UtcTime.now)).create

            service.logPoints(order).await
            service.logPoints(otherOrder).await

            assertCustomerIsLinked(customer.id, loyaltyProgram)
            assertOrderPointsAssigned(customer.id, loyaltyProgram, 1000)
          }
        }

        "if the order includes discounts" should {
          "award points based on the amount paid" in new LoyaltyProgramServiceSpecContext {
            val otherOrder = Factory
              .order(
                merchant,
                Some(rome),
                Some(customer),
                totalAmount = Some(10),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create
            Factory.orderItem(otherOrder, paymentStatus = Some(PaymentStatus.Paid), totalPriceAmount = Some(20)).create

            Factory
              .paymentTransaction(
                otherOrder,
                paymentType = Some(TransactionPaymentType.Cash),
                paymentDetails = Some(PaymentDetails(amount = Some(10))),
                `type` = Some(TransactionType.Payment),
              )
              .create

            val loyaltyProgram = Factory
              .loyaltyProgram(
                merchant,
                locations = Seq(rome),
                `type` = Some(LoyaltyProgramType.Spend),
                points = Some(1),
                minimumPurchaseAmount = Some(1),
                spendAmountForPoints = Some(1),
              )
              .create
            val loyaltyMembership =
              Factory.loyaltyMembership(globalCustomer, loyaltyProgram, merchantOptInAt = Some(UtcTime.now)).create

            service.logPoints(otherOrder).await

            assertCustomerIsLinked(customer.id, loyaltyProgram)
            assertOrderPointsAssigned(customer.id, loyaltyProgram, 10)
          }
        }

        "if it includes the purchase of a gift card" should {
          "not award points for the gift card" in new LoyaltyProgramServiceSpecContext {
            val otherOrder = Factory
              .order(
                merchant,
                Some(rome),
                Some(customer),
                totalAmount = Some(30),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create
            Factory.orderItem(otherOrder, paymentStatus = Some(PaymentStatus.Paid), totalPriceAmount = Some(10)).create

            val giftCardProduct = Factory.giftCardProduct(merchant).create
            Factory
              .orderItem(
                otherOrder,
                product = Some(giftCardProduct),
                paymentStatus = Some(PaymentStatus.Paid),
                totalPriceAmount = Some(20),
              )
              .create

            Factory
              .paymentTransaction(
                otherOrder,
                paymentType = Some(TransactionPaymentType.Cash),
                paymentDetails = Some(PaymentDetails(amount = Some(30))),
                `type` = Some(TransactionType.Payment),
              )
              .create

            val loyaltyProgram = Factory
              .loyaltyProgram(
                merchant,
                locations = Seq(rome),
                `type` = Some(LoyaltyProgramType.Spend),
                points = Some(1),
                minimumPurchaseAmount = Some(1),
                spendAmountForPoints = Some(1),
              )
              .create
            val loyaltyMembership =
              Factory.loyaltyMembership(globalCustomer, loyaltyProgram, merchantOptInAt = Some(UtcTime.now)).create

            service.logPoints(otherOrder).await

            assertCustomerIsLinked(customer.id, loyaltyProgram)
            assertOrderPointsAssigned(customer.id, loyaltyProgram, 10)

          }
        }

        "if it is paid for by a gift card" should {
          "award points as usual" in new LoyaltyProgramServiceSpecContext {
            val otherOrder = Factory
              .order(
                merchant,
                Some(rome),
                Some(customer),
                totalAmount = Some(20),
                paymentStatus = Some(PaymentStatus.Paid),
              )
              .create
            Factory.orderItem(otherOrder, paymentStatus = Some(PaymentStatus.Paid), totalPriceAmount = Some(20)).create
            Factory
              .paymentTransaction(
                otherOrder,
                paymentType = Some(TransactionPaymentType.GiftCard),
                paymentDetails = Some(PaymentDetails(amount = Some(20))),
                `type` = Some(TransactionType.Payment),
              )
              .create

            val loyaltyProgram = Factory
              .loyaltyProgram(
                merchant,
                locations = Seq(rome),
                `type` = Some(LoyaltyProgramType.Spend),
                points = Some(1),
                minimumPurchaseAmount = Some(1),
                spendAmountForPoints = Some(1),
              )
              .create
            val loyaltyMembership =
              Factory.loyaltyMembership(globalCustomer, loyaltyProgram, merchantOptInAt = Some(UtcTime.now)).create

            service.logPoints(otherOrder).await

            assertCustomerIsLinked(customer.id, loyaltyProgram)
            assertOrderPointsAssigned(customer.id, loyaltyProgram, 20)
          }
        }
      }
    }
  }
}
