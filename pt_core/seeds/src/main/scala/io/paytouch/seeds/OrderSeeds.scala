package io.paytouch.seeds

import java.util.UUID

import io.paytouch.core.data.model._
import io.paytouch.core.entities.{ OrderWorkflow, ResettableString }
import io.paytouch.seeds.IdsProvider._
import io.paytouch.seeds.SeedsQuantityProvider._
import org.scalacheck.Gen

import scala.concurrent._

object OrderSeeds extends Seeds {

  lazy val orderDao = daos.orderDao

  def load(
      customerLocations: Seq[CustomerLocationRecord],
      users: Seq[UserRecord],
    )(implicit
      user: UserRecord,
    ): Future[Seq[OrderRecord]] = {

    val orderIds = orderIdsPerEmail(user.email)
    val businessType = businessTypePerEmail(user.email)

    val orderIdsWithNoCustomers = orderIds.random(OrdersWithNoCustomer)

    val orders = orderIds.map { orderId =>
      val customerLocation = customerLocations.random
      val receivedAt = genZonedDateTime.instance
      val subtotalAmount = genBigDecimal.instance
      val discountAmount = genBigDecimal.instance
      val taxAmount = genBigDecimal.instance
      val tipAmount = genBigDecimal.instance
      val ticketDiscountAmount = genBigDecimal.instance
      val deliveryFeeAmount = Gen.option(genBigDecimal).instance
      val totalAmount =
        subtotalAmount + discountAmount + taxAmount + tipAmount - ticketDiscountAmount + deliveryFeeAmount
          .getOrElse(0)

      val orderType = genOrderType(businessType).instance
      val status = genOrderStatus.instance
      val orderWorkFlow = OrderWorkflow.getByOrderType(orderType, Seq.empty)
      val completedOrderWorkFlow = orderWorkFlow.take(orderWorkFlow.indexOf(status) + 1)

      val userId = users.random.id
      OrderUpdate(
        id = Some(orderId),
        merchantId = Some(user.merchantId),
        locationId = Some(customerLocation.locationId),
        deviceId = None,
        userId = Some(userId),
        customerId = if (orderIdsWithNoCustomers.contains(orderId)) None else Some(customerLocation.customerId),
        deliveryAddressId = None,
        onlineOrderAttributeId = None,
        tag = Gen.option(genWord).instance,
        source = Some(genSource.instance),
        `type` = Some(orderType),
        paymentType = Some(genOrderPaymentType.instance),
        totalAmount = Some(totalAmount),
        subtotalAmount = Some(subtotalAmount),
        discountAmount = Some(discountAmount),
        taxAmount = Some(taxAmount),
        tipAmount = Some(tipAmount),
        ticketDiscountAmount = Some(ticketDiscountAmount),
        deliveryFeeAmount = deliveryFeeAmount,
        customerNotes =
          Seq(CustomerNote(UUID.randomUUID, randomWords(n = 10, allCapitalized = false), genZonedDateTime.instance)),
        merchantNotes = Seq(
          MerchantNote(
            id = UUID.randomUUID,
            userId = userId,
            randomWords(n = 5, allCapitalized = false),
            genZonedDateTime.instance,
          ),
        ),
        paymentStatus = Some(genPaymentStatus.instance),
        status = Some(status),
        fulfillmentStatus = Some(genFulfillmentStatus.instance),
        statusTransitions = Some(completedOrderWorkFlow.map { status =>
          StatusTransition(UUID.randomUUID, status, genZonedDateTime.instance)
        }),
        isInvoice = Some(genBoolean.instance),
        isFiscal = Some(genBoolean.instance),
        version = Some(2),
        seating = None,
        deliveryProvider = None,
        deliveryProviderId = None,
        deliveryProviderNumber = None,
        receivedAt = Some(genZonedDateTimeInThePast.instance),
        receivedAtTz = Some(genZonedDateTimeInThePast.instance),
        completedAt = if (genBoolean.instance) Some(receivedAt.plusHours(genInt.instance)) else None,
        completedAtTz = if (genBoolean.instance) Some(receivedAt.plusHours(genInt.instance)) else None,
      )
    }

    orderDao.bulkUpsert(orders).extractRecords
  }
}
