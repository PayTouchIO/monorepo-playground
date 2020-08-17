package io.paytouch.core.entities

import java.util.Currency
import java.util.UUID

import org.specs2.specification.{ Scope => SpecScope }

import io.paytouch.core.data.model._
import io.paytouch.core.data.model.enums._
import io.paytouch.core.utils.PaytouchSpec

class OrderPointsDataSpec extends PaytouchSpec {
  abstract class OrderPointsDataSpecContext extends SpecScope

  "totalAmountForPoints" should {
    "when payment is pending" should {
      "return 0" in new OrderPointsDataSpecContext with Fixtures {
        @scala.annotation.nowarn("msg=Auto-application")
        val orderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(15),
            productType = Some(ArticleType.Simple),
            paymentStatus = Some(PaymentStatus.Pending),
          )

        val pointsData = OrderPointsData.extract(order, Seq.empty, Seq(orderItem)).get
        pointsData.totalAmountForPoints ==== 0
      }
    }

    "when the total amount is zero" should {
      "return 0" in new OrderPointsDataSpecContext with Fixtures {
        @scala.annotation.nowarn("msg=Auto-application")
        val orderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(0),
            productType = Some(ArticleType.Simple),
            paymentStatus = Some(PaymentStatus.Paid),
          )

        val pointsData = OrderPointsData.extract(order, Seq.empty, Seq(orderItem)).get
        pointsData.totalAmountForPoints ==== 0
      }
    }

    "when payment is void" should {
      "return 0" in new OrderPointsDataSpecContext with Fixtures {
        @scala.annotation.nowarn("msg=Auto-application")
        val orderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(15),
            productType = Some(ArticleType.Simple),
            paymentStatus = Some(PaymentStatus.Pending),
          )

        @scala.annotation.nowarn("msg=Auto-application")
        val transaction = random[PaymentTransaction].copy(
          `type` = Some(TransactionType.Void),
          paymentType = Some(TransactionPaymentType.CreditCard),
          paymentDetails = Some(random[PaymentDetails].copy(amount = Some(17), tipAmount = 2)),
        )

        val pointsData = OrderPointsData.extract(order, Seq(transaction), Seq(orderItem)).get
        pointsData.totalAmountForPoints ==== 0
      }
    }

    "when payment is paid" should {
      "return the amount" in new OrderPointsDataSpecContext with Fixtures {
        @scala.annotation.nowarn("msg=Auto-application")
        val orderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(15),
            productType = Some(ArticleType.Simple),
            paymentStatus = Some(PaymentStatus.Paid),
          )

        @scala.annotation.nowarn("msg=Auto-application")
        val transaction = random[PaymentTransaction].copy(
          `type` = Some(TransactionType.Payment),
          paymentType = Some(TransactionPaymentType.CreditCard),
          paymentDetails = Some(random[PaymentDetails].copy(amount = Some(17), tipAmount = 2)),
        )

        val pointsData = OrderPointsData.extract(order, Seq(transaction), Seq(orderItem)).get
        pointsData.totalAmountForPoints ==== 15
      }
    }

    "when payment is paid and there is no product type" should {
      "return the amount" in new OrderPointsDataSpecContext with Fixtures {
        @scala.annotation.nowarn("msg=Auto-application")
        val orderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(15),
            productType = None,
            paymentStatus = Some(PaymentStatus.Paid),
          )

        @scala.annotation.nowarn("msg=Auto-application")
        val transaction = random[PaymentTransaction].copy(
          `type` = Some(TransactionType.Payment),
          paymentType = Some(TransactionPaymentType.CreditCard),
          paymentDetails = Some(random[PaymentDetails].copy(amount = Some(17), tipAmount = 2)),
        )

        val pointsData = OrderPointsData.extract(order, Seq(transaction), Seq(orderItem)).get
        pointsData.totalAmountForPoints ==== 15
      }
    }

    "when payment is paid and the order is paid with a gift card" should {
      "return the amount" in new OrderPointsDataSpecContext with Fixtures {
        @scala.annotation.nowarn("msg=Auto-application")
        val orderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(15),
            productType = Some(ArticleType.Simple),
            paymentStatus = Some(PaymentStatus.Paid),
          )

        @scala.annotation.nowarn("msg=Auto-application")
        val transaction = random[PaymentTransaction].copy(
          `type` = Some(TransactionType.Payment),
          paymentType = Some(TransactionPaymentType.GiftCard),
          paymentDetails = Some(random[PaymentDetails].copy(amount = Some(17), tipAmount = 2)),
        )

        val pointsData = OrderPointsData.extract(order, Seq(transaction), Seq(orderItem)).get
        pointsData.totalAmountForPoints ==== 15
      }
    }

    "when payment is successful and the order is for a gift card" should {
      "return zero" in new OrderPointsDataSpecContext with Fixtures {
        @scala.annotation.nowarn("msg=Auto-application")
        val orderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(15),
            productType = Some(ArticleType.GiftCard),
            paymentStatus = Some(PaymentStatus.Paid),
          )

        @scala.annotation.nowarn("msg=Auto-application")
        val transaction = random[PaymentTransaction].copy(
          `type` = Some(TransactionType.Payment),
          paymentType = Some(TransactionPaymentType.CreditCard),
          paymentDetails = Some(random[PaymentDetails].copy(amount = Some(17), tipAmount = 2)),
        )

        val pointsData = OrderPointsData.extract(order, Seq(transaction), Seq(orderItem)).get
        pointsData.totalAmountForPoints ==== 0
      }
    }

    "when payment is successful and the order is for a gift card and other items" should {
      "return the amount for eligible items" in new OrderPointsDataSpecContext with Fixtures {
        @scala.annotation.nowarn("msg=Auto-application")
        val giftCardOrderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(12.50),
            productType = Some(ArticleType.GiftCard),
            paymentStatus = Some(PaymentStatus.Paid),
          )

        @scala.annotation.nowarn("msg=Auto-application")
        val simpleOrderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(10.45),
            productType = Some(ArticleType.Simple),
            paymentStatus = Some(PaymentStatus.Paid),
          )

        @scala.annotation.nowarn("msg=Auto-application")
        val transaction = random[PaymentTransaction].copy(
          `type` = Some(TransactionType.Payment),
          paymentType = Some(TransactionPaymentType.CreditCard),
          paymentDetails = Some(random[PaymentDetails].copy(amount = Some(24.95), tipAmount = 2)),
        )

        val pointsData = OrderPointsData.extract(order, Seq(transaction), Seq(giftCardOrderItem, simpleOrderItem)).get
        pointsData.totalAmountForPoints ==== 10.45
      }
    }
  }

  "eligibleAmountForTransaction" should {
    "when the transaction is eligible" should {
      "return the eligible amount" in new OrderPointsDataSpecContext with Fixtures {
        @scala.annotation.nowarn("msg=Auto-application")
        val orderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(15),
            productType = Some(ArticleType.Simple),
            paymentStatus = Some(PaymentStatus.Paid),
          )

        @scala.annotation.nowarn("msg=Auto-application")
        val transaction = random[PaymentTransaction].copy(
          `type` = Some(TransactionType.Payment),
          paymentType = Some(TransactionPaymentType.CreditCard),
          paymentDetails = Some(random[PaymentDetails].copy(amount = Some(17), tipAmount = 2)),
        )

        val pointsData = OrderPointsData.extract(order, Seq(transaction), Seq(orderItem)).get
        val dataTransaction = pointsData.paymentTransactions.find(_.id == transaction.id).get
        pointsData.eligibleAmountForTransaction(dataTransaction) ==== 15
      }
    }

    "when the transaction amount is 0" should {
      "return zero" in new OrderPointsDataSpecContext with Fixtures {
        @scala.annotation.nowarn("msg=Auto-application")
        val orderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(15),
            productType = Some(ArticleType.Simple),
            paymentStatus = Some(PaymentStatus.Paid),
          )

        @scala.annotation.nowarn("msg=Auto-application")
        val cardTransaction = random[PaymentTransaction].copy(
          id = UUID.randomUUID,
          `type` = Some(TransactionType.Payment),
          paymentType = Some(TransactionPaymentType.CreditCard),
          paymentDetails = Some(random[PaymentDetails].copy(amount = Some(15), tipAmount = 0)),
        )

        @scala.annotation.nowarn("msg=Auto-application")
        val cashTransaction = random[PaymentTransaction].copy(
          id = UUID.randomUUID,
          `type` = Some(TransactionType.Payment),
          paymentType = Some(TransactionPaymentType.Cash),
          paymentDetails = Some(random[PaymentDetails].copy(amount = Some(2), tipAmount = 2)),
        )

        val pointsData = OrderPointsData.extract(order, Seq(cardTransaction, cashTransaction), Seq(orderItem)).get
        val dataTransaction = pointsData.paymentTransactions.find(_.id == cashTransaction.id).get
        pointsData.eligibleAmountForTransaction(dataTransaction) ==== 0
      }
    }

    "when the order amount is 0" should {
      "return 0" in new OrderPointsDataSpecContext with Fixtures {
        @scala.annotation.nowarn("msg=Auto-application")
        val orderItem =
          random[OrderItemRecord].copy(
            totalPriceAmount = Some(15),
            productType = Some(ArticleType.GiftCard),
            paymentStatus = Some(PaymentStatus.Paid),
          )

        @scala.annotation.nowarn("msg=Auto-application")
        val transaction = random[PaymentTransaction].copy(
          `type` = Some(TransactionType.Payment),
          paymentType = Some(TransactionPaymentType.CreditCard),
          paymentDetails = Some(random[PaymentDetails].copy(amount = Some(17), tipAmount = 2)),
        )

        val pointsData = OrderPointsData.extract(order, Seq(transaction), Seq(orderItem)).get
        val dataTransaction = pointsData.paymentTransactions.find(_.id == transaction.id).get
        pointsData.eligibleAmountForTransaction(dataTransaction) ==== 0
      }
    }
  }

  trait Fixtures {
    @scala.annotation.nowarn("msg=Auto-application")
    lazy val order =
      random[OrderRecord].copy(
        customerId = Some(UUID.randomUUID),
        locationId = Some(UUID.randomUUID),
        paymentStatus = Some(PaymentStatus.Paid),
      )
  }
}
