package io.paytouch.core.services

import java.util.UUID

import io.paytouch.core.data.model.{ ArticleRecord, LoyaltyPointsHistoryUpdate }
import io.paytouch.core.data.model.enums.{
  ArticleType,
  LoyaltyPointsHistoryType,
  PaymentStatus,
  TransactionPaymentType,
  TransactionType,
}
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.{ LoyaltyProgramType }
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }
import io.paytouch.core.data.model.{ OrderItemRecord }

@scala.annotation.nowarn("msg=Auto-application")
class LoyaltyPointsHistoryServiceSpec extends ServiceDaoSpec {
  abstract class LoyaltyPointsHistoryServiceSpecContext extends ServiceDaoSpecContext {
    val service = loyaltyPointsHistoryService

    val customer = Factory.globalCustomer().create
    val loyaltyProgramFrequency =
      Factory
        .loyaltyProgram(
          merchant,
          locations = locations,
          `type` = Some(LoyaltyProgramType.Frequency),
          points = Some(100),
          minimumPurchaseAmount = Some(10),
        )
        .create
    val loyaltyProgramSpend =
      Factory
        .loyaltyProgram(
          merchant,
          locations = locations,
          `type` = Some(LoyaltyProgramType.Spend),
          points = Some(100),
          minimumPurchaseAmount = Some(10),
          spendAmountForPoints = Some(5),
        )
        .create

    val loyaltyMembership =
      Factory.loyaltyMembership(customer, loyaltyProgramFrequency, merchantOptInAt = Some(UtcTime.now)).create

    val baseOrderPointsData = OrderPointsData(
      id = UUID.randomUUID,
      customerId = customer.id,
      locationId = rome.id,
      paymentStatus = PaymentStatus.Pending,
      paymentTransactions = Seq.empty,
      orderItems = Seq.empty,
    )

    def assertHistoryUpdate(
        historyUpdate: LoyaltyPointsHistoryUpdate,
        objectId: UUID,
        `type`: LoyaltyPointsHistoryType,
        points: Int,
      ) = {
      historyUpdate.loyaltyMembershipId ==== Some(loyaltyMembership.id)
      historyUpdate.objectId ==== Some(objectId)
      historyUpdate.`type` ==== Some(`type`)
      historyUpdate.points ==== Some(points)
    }
  }

  "LoyaltyPointsHistoryService" in {
    "convertToUpdates" in {
      "if loyalty program is available in order location" in {
        "if customer is enrolled in the program" in {
          "if loyalty program is frequency based" in {
            "if order total is greater than loyalty program minimumOrderAmount" should {
              "if order is in status paid" in {
                "return a type=Visit history update" in new LoyaltyPointsHistoryServiceSpecContext {
                  val purchaseOrderItem = OrderPointsDataOrderItem
                    .extractFromRecord(
                      random[OrderItemRecord].copy(
                        totalPriceAmount = Some(20),
                        productType = Some(ArticleType.Simple),
                        paymentStatus = Some(PaymentStatus.Paid),
                      ),
                    )
                    .get

                  val purchasePaymentTransaction = random[OrderPointsDataTransaction].copy(
                    `type` = TransactionType.Payment,
                    paymentType = TransactionPaymentType.Cash,
                    amount = 20,
                    tipAmount = 0,
                  )

                  val orderPointsData = baseOrderPointsData.copy(
                    paymentStatus = PaymentStatus.Paid,
                    paymentTransactions = Seq(purchasePaymentTransaction),
                    orderItems = Seq(purchaseOrderItem),
                  )

                  val result =
                    service.convertToUpdates(loyaltyMembership, loyaltyProgramFrequency, orderPointsData).await

                  result.size ==== 1

                  assertHistoryUpdate(
                    result.head,
                    objectId = orderPointsData.id,
                    `type` = LoyaltyPointsHistoryType.Visit,
                    points = loyaltyProgramFrequency.points,
                  )
                }
              }
              "if order is in status refunded or voided" should {
                "return a type=Visit and a type=VisitCancel history updates" in new LoyaltyPointsHistoryServiceSpecContext {
                  val purchaseOrderItem = OrderPointsDataOrderItem
                    .extractFromRecord(
                      random[OrderItemRecord].copy(
                        totalPriceAmount = Some(20),
                        productType = Some(ArticleType.Simple),
                        paymentStatus = Some(PaymentStatus.Refunded),
                      ),
                    )
                    .get

                  val purchasePaymentTransaction = random[OrderPointsDataTransaction].copy(
                    `type` = TransactionType.Payment,
                    paymentType = TransactionPaymentType.Cash,
                    amount = 20,
                    tipAmount = 0,
                  )
                  val refundPaymentTransaction = random[OrderPointsDataTransaction].copy(
                    `type` = TransactionType.Refund,
                    paymentType = TransactionPaymentType.Cash,
                    amount = 10,
                    tipAmount = 0,
                  )
                  val orderPointsData = baseOrderPointsData.copy(
                    paymentStatus = PaymentStatus.Refunded,
                    paymentTransactions = Seq(purchasePaymentTransaction, refundPaymentTransaction),
                    orderItems = Seq(purchaseOrderItem),
                  )
                  val result =
                    service.convertToUpdates(loyaltyMembership, loyaltyProgramFrequency, orderPointsData).await

                  result.size ==== 2

                  assertHistoryUpdate(
                    result.head,
                    objectId = orderPointsData.id,
                    `type` = LoyaltyPointsHistoryType.Visit,
                    points = loyaltyProgramFrequency.points,
                  )

                  assertHistoryUpdate(
                    result(1),
                    objectId = orderPointsData.id,
                    `type` = LoyaltyPointsHistoryType.VisitCancel,
                    points = -loyaltyProgramFrequency.points,
                  )
                }
              }
            }
            "if order total is less than loyalty program minimumOrderAmount" should {
              "return no change" in new LoyaltyPointsHistoryServiceSpecContext {
                val purchaseOrderItem = OrderPointsDataOrderItem
                  .extractFromRecord(
                    random[OrderItemRecord].copy(
                      totalPriceAmount = Some(5),
                      productType = Some(ArticleType.Simple),
                      paymentStatus = Some(PaymentStatus.Paid),
                    ),
                  )
                  .get

                val purchasePaymentTransaction = random[OrderPointsDataTransaction].copy(
                  `type` = TransactionType.Payment,
                  paymentType = TransactionPaymentType.Cash,
                  amount = 5,
                  tipAmount = 0,
                )
                val orderPointsData = baseOrderPointsData.copy(
                  paymentStatus = PaymentStatus.Paid,
                  paymentTransactions = Seq(purchasePaymentTransaction),
                  orderItems = Seq(purchaseOrderItem),
                )
                val result =
                  service.convertToUpdates(loyaltyMembership, loyaltyProgramFrequency, orderPointsData).await

                result.size ==== 0
              }
            }
            "if order is in status partially paid" should {
              "return no change" in new LoyaltyPointsHistoryServiceSpecContext {
                val purchaseOrderItem = OrderPointsDataOrderItem
                  .extractFromRecord(
                    random[OrderItemRecord].copy(
                      totalPriceAmount = Some(50),
                      productType = Some(ArticleType.Simple),
                      paymentStatus = Some(PaymentStatus.PartiallyPaid),
                    ),
                  )
                  .get

                val purchasePaymentTransaction = random[OrderPointsDataTransaction].copy(
                  `type` = TransactionType.Payment,
                  paymentType = TransactionPaymentType.Cash,
                  amount = 25,
                  tipAmount = 0,
                )
                val orderPointsData = baseOrderPointsData.copy(
                  paymentStatus = PaymentStatus.PartiallyPaid,
                  paymentTransactions = Seq(purchasePaymentTransaction),
                  orderItems = Seq(purchaseOrderItem),
                )
                val result =
                  service.convertToUpdates(loyaltyMembership, loyaltyProgramFrequency, orderPointsData).await

                result.size ==== 0
              }
            }
          }
          "if loyalty program is spend based" in {
            "if order total is greater than loyalty program minimumOrderAmount" should {
              "if order is in status paid or partially paid" in {
                "return a type=Spend history update" in new LoyaltyPointsHistoryServiceSpecContext {
                  val purchaseOrderItem = OrderPointsDataOrderItem
                    .extractFromRecord(
                      random[OrderItemRecord].copy(
                        totalPriceAmount = Some(20),
                        productType = Some(ArticleType.Simple),
                        paymentStatus = Some(PaymentStatus.Paid),
                      ),
                    )
                    .get

                  val purchasePaymentTransaction = random[OrderPointsDataTransaction].copy(
                    `type` = TransactionType.Payment,
                    paymentType = TransactionPaymentType.Cash,
                    amount = 20,
                    tipAmount = 0,
                  )
                  val orderPointsData = baseOrderPointsData.copy(
                    paymentStatus = PaymentStatus.Paid,
                    paymentTransactions = Seq(purchasePaymentTransaction),
                    orderItems = Seq(purchaseOrderItem),
                  )
                  val result = service.convertToUpdates(loyaltyMembership, loyaltyProgramSpend, orderPointsData).await

                  result.size ==== 1

                  assertHistoryUpdate(
                    result.head,
                    objectId = purchasePaymentTransaction.id,
                    `type` = LoyaltyPointsHistoryType.SpendTransaction,
                    points = 400,
                  )
                }
              }
              "if order is in status refunded or voided or canceled" should {
                "return a type=SpendTransaction history update per Payment payment transaction and" +
                  "a type=SpendRefund history update per each Refund/Void payment transaction" in new LoyaltyPointsHistoryServiceSpecContext {
                  val purchaseOrderItem = OrderPointsDataOrderItem
                    .extractFromRecord(
                      random[OrderItemRecord].copy(
                        totalPriceAmount = Some(32),
                        productType = Some(ArticleType.Simple),
                        paymentStatus = Some(PaymentStatus.Paid),
                      ),
                    )
                    .get

                  val purchaseCashPaymentTransaction = random[OrderPointsDataTransaction].copy(
                    id = UUID.randomUUID,
                    `type` = TransactionType.Payment,
                    paymentType = TransactionPaymentType.Cash,
                    amount = 27,
                    tipAmount = 7,
                  )
                  val purchaseGiftCardPaymentTransaction = random[OrderPointsDataTransaction].copy(
                    id = UUID.randomUUID,
                    `type` = TransactionType.Payment,
                    paymentType = TransactionPaymentType.GiftCard,
                    amount = 5,
                    tipAmount = 0,
                  )
                  val refundPaymentTransaction1 = random[OrderPointsDataTransaction].copy(
                    id = UUID.randomUUID,
                    `type` = TransactionType.Refund,
                    paymentType = TransactionPaymentType.Cash,
                    amount = 10,
                    tipAmount = 0,
                  )
                  val refundPaymentTransaction2 = random[OrderPointsDataTransaction].copy(
                    id = UUID.randomUUID,
                    `type` = TransactionType.Refund,
                    amount = 5,
                    tipAmount = 0,
                  )
                  val orderPointsData = baseOrderPointsData.copy(
                    paymentStatus = PaymentStatus.Refunded,
                    paymentTransactions = Seq(
                      purchaseCashPaymentTransaction,
                      purchaseGiftCardPaymentTransaction,
                      refundPaymentTransaction1,
                      refundPaymentTransaction2,
                    ),
                    orderItems = Seq(purchaseOrderItem),
                  )
                  val result = service.convertToUpdates(loyaltyMembership, loyaltyProgramSpend, orderPointsData).await

                  result.size ==== 4

                  assertHistoryUpdate(
                    result.head,
                    objectId = purchaseCashPaymentTransaction.id,
                    `type` = LoyaltyPointsHistoryType.SpendTransaction,
                    points = 400,
                  )

                  assertHistoryUpdate(
                    result(1),
                    objectId = purchaseGiftCardPaymentTransaction.id,
                    `type` = LoyaltyPointsHistoryType.SpendTransaction,
                    points = 100,
                  )

                  assertHistoryUpdate(
                    result(2),
                    objectId = refundPaymentTransaction1.id,
                    `type` = LoyaltyPointsHistoryType.SpendRefund,
                    points = -200,
                  )

                  assertHistoryUpdate(
                    result(3),
                    objectId = refundPaymentTransaction2.id,
                    `type` = LoyaltyPointsHistoryType.SpendRefund,
                    points = -100,
                  )
                }
              }
            }
            "if order total is less than loyalty program minimumOrderAmount" should {
              "return no change" in new LoyaltyPointsHistoryServiceSpecContext {
                val purchaseOrderItem = OrderPointsDataOrderItem
                  .extractFromRecord(
                    random[OrderItemRecord].copy(
                      totalPriceAmount = Some(5),
                      productType = Some(ArticleType.Simple),
                      paymentStatus = Some(PaymentStatus.Paid),
                    ),
                  )
                  .get

                val purchasePaymentTransaction = random[OrderPointsDataTransaction].copy(
                  `type` = TransactionType.Payment,
                  paymentType = TransactionPaymentType.Cash,
                  amount = 5,
                  tipAmount = 0,
                )
                val orderPointsData = baseOrderPointsData.copy(
                  paymentStatus = PaymentStatus.Paid,
                  paymentTransactions = Seq(purchasePaymentTransaction),
                  orderItems = Seq(purchaseOrderItem),
                )
                val result = service.convertToUpdates(loyaltyMembership, loyaltyProgramSpend, orderPointsData).await

                result.size ==== 0
              }
            }
            "if order is in status partially paid" should {
              "return change for single transaction" in new LoyaltyPointsHistoryServiceSpecContext {
                val purchaseOrderItem = OrderPointsDataOrderItem
                  .extractFromRecord(
                    random[OrderItemRecord].copy(
                      totalPriceAmount = Some(50),
                      productType = Some(ArticleType.Simple),
                      paymentStatus = Some(PaymentStatus.PartiallyPaid),
                    ),
                  )
                  .get

                val purchasePaymentTransaction = random[OrderPointsDataTransaction].copy(
                  `type` = TransactionType.Payment,
                  paymentType = TransactionPaymentType.Cash,
                  amount = 25,
                  tipAmount = 0,
                )
                val orderPointsData = baseOrderPointsData.copy(
                  paymentStatus = PaymentStatus.PartiallyPaid,
                  paymentTransactions = Seq(purchasePaymentTransaction),
                  orderItems = Seq(purchaseOrderItem),
                )
                val result = service.convertToUpdates(loyaltyMembership, loyaltyProgramSpend, orderPointsData).await

                result.size ==== 1

                assertHistoryUpdate(
                  result.head,
                  objectId = purchasePaymentTransaction.id,
                  `type` = LoyaltyPointsHistoryType.SpendTransaction,
                  points = 500,
                )

              }
            }
          }
        }
      }
    }
  }
}
