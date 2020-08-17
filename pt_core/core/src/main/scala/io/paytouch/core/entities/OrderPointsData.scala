package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.{ OrderItemRecord, OrderRecord }
import io.paytouch.core.data.model.enums.{ ArticleType, PaymentStatus, TransactionPaymentType, TransactionType }

import scala.util.Try

final case class OrderPointsData(
    id: UUID,
    customerId: UUID,
    locationId: UUID,
    paymentStatus: PaymentStatus,
    paymentTransactions: Seq[OrderPointsDataTransaction],
    orderItems: Seq[OrderPointsDataOrderItem],
  ) {
  lazy val payments = paymentTransactions.filter(_.`type` == TransactionType.Payment)
  lazy val totalPaymentsAmount = payments.map(_.amountForPoints).sum

  lazy val totalOrderItemsAmount = orderItems.map(_.amount).sum
  lazy val eligibleOrderItemsAmount = orderItems.map(_.amountForPoints).sum

  // The amount of all transactions created so far that are eligible for points
  lazy val totalAmountForPoints: BigDecimal =
    Try(totalPaymentsAmount / totalOrderItemsAmount * eligibleOrderItemsAmount).getOrElse(0)

  def eligibleAmountForTransaction(transaction: OrderPointsDataTransaction): BigDecimal =
    // The proportional amount of this transaction that is eligible for points
    Try(totalAmountForPoints / totalPaymentsAmount * transaction.amountForPoints).getOrElse(0)
}

object OrderPointsData {
  implicit def fromOrderEntity(order: Order): Option[OrderPointsData] =
    for {
      customerId <- order.customer.map(_.id)
      locationId <- order.location.map(_.id)
      paymentStatus <- order.paymentStatus
    } yield {
      val paymentTransactions =
        order.paymentTransactions.getOrElse(Seq.empty).flatMap(OrderPointsDataTransaction.extract)
      val orderItems =
        order.items.getOrElse(Seq.empty).flatMap(OrderPointsDataOrderItem.extractFromItem)
      OrderPointsData(
        id = order.id,
        customerId = customerId,
        locationId = locationId,
        paymentStatus = paymentStatus,
        paymentTransactions = paymentTransactions,
        orderItems = orderItems,
      )
    }

  def extract(
      order: OrderRecord,
      paymentTransactions: Seq[PaymentTransaction],
      orderItems: Seq[OrderItemRecord],
    ): Option[OrderPointsData] =
    for {
      customerId <- order.customerId
      locationId <- order.locationId
      paymentStatus <- order.paymentStatus
    } yield {
      val dataPaymentTransactions = paymentTransactions.flatMap(OrderPointsDataTransaction.extract)
      val dataOrderItems = orderItems.flatMap(OrderPointsDataOrderItem.extractFromRecord)
      OrderPointsData(
        id = order.id,
        customerId = customerId,
        locationId = locationId,
        paymentStatus = paymentStatus,
        paymentTransactions = dataPaymentTransactions,
        orderItems = dataOrderItems,
      )
    }
}

final case class OrderPointsDataTransaction(
    id: UUID,
    paymentType: TransactionPaymentType,
    `type`: TransactionType,
    amount: BigDecimal,
    tipAmount: BigDecimal,
  ) {
  lazy val amountForPoints = amount - tipAmount
}

object OrderPointsDataTransaction {
  def extract(paymentTransaction: PaymentTransaction): Option[OrderPointsDataTransaction] =
    for {
      ptPaymentType <- paymentTransaction.paymentType
      ptType <- paymentTransaction.`type`
    } yield {
      val amount = paymentTransaction.paymentDetails.flatMap(_.amount).getOrElse[BigDecimal](0)
      val tipAmount = paymentTransaction.paymentDetails.map(_.tipAmount).getOrElse[BigDecimal](0)
      OrderPointsDataTransaction(
        id = paymentTransaction.id,
        paymentType = ptPaymentType,
        `type` = ptType,
        amount = amount,
        tipAmount = tipAmount,
      )
    }
}

final case class OrderPointsDataOrderItem(
    id: UUID,
    productType: Option[ArticleType],
    amount: BigDecimal,
  ) {
  lazy val amountForPoints: BigDecimal = productType match {
    case Some(ArticleType.GiftCard) => 0
    case _                          => amount
  }
}

object OrderPointsDataOrderItem {
  def extractFromItem(orderItem: OrderItem): Option[OrderPointsDataOrderItem] =
    orderItem.totalPrice.map { totalPrice =>
      OrderPointsDataOrderItem(
        id = orderItem.id,
        productType = orderItem.productType,
        amount = totalPrice.amount,
      )
    }

  def extractFromRecord(orderItem: OrderItemRecord): Option[OrderPointsDataOrderItem] =
    orderItem.totalPriceAmount.map { totalPriceAmount =>
      OrderPointsDataOrderItem(
        id = orderItem.id,
        productType = orderItem.productType,
        amount = totalPriceAmount,
      )
    }
}
