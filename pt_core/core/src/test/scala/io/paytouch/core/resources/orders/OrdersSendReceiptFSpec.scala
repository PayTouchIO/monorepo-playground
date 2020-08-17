package io.paytouch.core.resources.orders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MalformedRequestContentRejection

import io.paytouch.core.data.model.{ LocationRecord, StatusTransition => StatusTransitionModel }
import io.paytouch.core.data.model.enums.{ OrderStatus, PaymentStatus }
import io.paytouch.core.entities.SendReceiptData
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class OrdersSendReceiptFSpec extends OrdersFSpec {

  abstract class OrdersSendReceiptFSpecContext extends OrderResourceFSpecContext {
    val customerLocationDao = daos.customerLocationDao
    val customerMerchantDao = daos.customerMerchantDao

    val statusTransitionModels =
      Seq(StatusTransitionModel(id = UUID.randomUUID, status = OrderStatus.Completed, createdAt = UtcTime.now))

    def assertCustomerMatchesReceiptRecipient(customerId: UUID, sendReceiptData: SendReceiptData) = {
      val createdCustomer =
        customerMerchantDao.findByIdAndMerchantId(customerId, merchant.id).await.get
      createdCustomer.email ==== Some(sendReceiptData.recipientEmail)
    }

    def assertCustomerIsLinkedToLocation(
        customerId: UUID,
        location: LocationRecord,
        totalSpendAmount: Option[BigDecimal],
      ) = {
      val customerLocation = customerLocationDao.findOneByItemIdAndLocationId(customerId, location.id).await.get
      location.id ==== customerLocation.locationId
      customerLocation.totalVisits ==== 1
      totalSpendAmount ==== Some(customerLocation.totalSpendAmount)
    }

    def assertCustomerLinkedToOrder(customerId: UUID, orderId: UUID) = {
      val order = orderDao.findById(orderId).await.get
      order.customerId.get ==== customerId
    }
  }

  "POST /v1/orders.send_receipt?order_id=$" in {
    "if request has valid token" in {

      "if the order belongs to the current merchant" should {
        "if order location is not accessible by current user" should {
          "return 404" in new OrdersSendReceiptFSpecContext {
            val newYork = Factory.location(merchant).create
            val sendReceiptData = random[SendReceiptData]
            val order = Factory.order(merchant, location = Some(newYork)).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid)).create
            Factory.paymentTransaction(order).create
            Post(s"/v1/orders.send_receipt?order_id=${order.id}", sendReceiptData)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
              assertErrorCode("NonAccessibleLocationIds")
            }
          }
        }

        "if email is invalid" should {
          "return 400" in new OrdersSendReceiptFSpecContext {
            val sendReceiptData = SendReceiptData(recipientEmail = "wrongemail")
            val order = Factory.order(merchant, location = Some(rome)).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid)).create
            Post(s"/v1/orders.send_receipt?order_id=${order.id}", sendReceiptData)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertErrorCodesAtLeastOnce("InvalidEmail", "NoPaymentTransactionsForOrderId")
            }
          }
        }

        "if the payment_transaction_id is not set and there are no payment transactions" should {
          "return 400" in new OrdersSendReceiptFSpecContext {
            val sendReceiptData = random[SendReceiptData]
            val order = Factory.order(merchant, location = Some(rome)).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid)).create
            Post(s"/v1/orders.send_receipt?order_id=${order.id}", sendReceiptData)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("NoPaymentTransactionsForOrderId")
            }
          }
        }

        "if the payment_transaction_id is set and it's not accessible" should {
          "return 404" in new OrdersSendReceiptFSpecContext {
            val sendReceiptData = random[SendReceiptData]
            val order = Factory.order(merchant, location = Some(rome)).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid)).create
            Post(
              s"/v1/orders.send_receipt?order_id=${order.id}&payment_transaction_id=${UUID.randomUUID}",
              sendReceiptData,
            ).addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
              assertErrorCode("NonAccessiblePaymentTransactionIds")
            }
          }
        }

        "if order has no location" should {
          "return 400" in new OrdersSendReceiptFSpecContext {
            val sendReceiptData = random[SendReceiptData]
            val order = Factory.order(merchant, location = None, paymentStatus = Some(PaymentStatus.Paid)).create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid)).create
            Post(s"/v1/orders.send_receipt?order_id=${order.id}", sendReceiptData)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCodesAtLeastOnce("NoPaymentTransactionsForOrderId", "InvalidOrderForSendReceipt")
            }
          }
        }

        "if order has no customer" should {
          "send the notification, create a customer with the given email and link it to the order" in new OrdersSendReceiptFSpecContext {
            val sendReceiptData = SendReceiptData(randomEmail)
            val order =
              Factory
                .order(
                  merchant,
                  location = Some(rome),
                  statusTransitions = Some(statusTransitionModels),
                  totalAmount = Some(BigDecimal(15)),
                )
                .create
            val orderItem =
              Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid), totalPriceAmount = Some(15)).create
            Factory.paymentTransaction(order).create

            Post(s"/v1/orders.send_receipt?order_id=${order.id}", sendReceiptData)
              .addHeader(authorizationHeader) ~> routes ~> check {
              val updatedOrder = orderDao.findById(order.id).await.get
              updatedOrder.customerId must beSome
              assertCustomerMatchesReceiptRecipient(updatedOrder.customerId.get, sendReceiptData)
              assertCustomerIsLinkedToLocation(updatedOrder.customerId.get, rome, totalSpendAmount = order.totalAmount)
            }
          }
        }

        "if order has a customer without email" should {
          "send the notification and update the customer with the given email" in new OrdersSendReceiptFSpecContext {
            val sendReceiptData = SendReceiptData(randomEmail)
            val globalCustomer = Factory.globalCustomerWithEmail(email = None).create
            val originalCustomer = Factory.customerMerchant(merchant, globalCustomer).create
            val order = Factory
              .order(
                merchant,
                customer = Some(originalCustomer),
                location = Some(rome),
                statusTransitions = Some(statusTransitionModels),
                totalAmount = Some(BigDecimal(15)),
              )
              .create
            val orderItem =
              Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid), totalPriceAmount = Some(15)).create
            Factory.paymentTransaction(order).create

            Post(s"/v1/orders.send_receipt?order_id=${order.id}", sendReceiptData)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertCustomerMatchesReceiptRecipient(originalCustomer.id, sendReceiptData)
            }
          }

          "send the notification and update the customer with the given email filtered by payment transaction id" in new OrdersSendReceiptFSpecContext {
            val sendReceiptData = SendReceiptData(randomEmail)
            val globalCustomer = Factory.globalCustomerWithEmail(email = None).create
            val originalCustomer = Factory.customerMerchant(merchant, globalCustomer).create
            val order = Factory
              .order(
                merchant,
                customer = Some(originalCustomer),
                location = Some(rome),
                statusTransitions = Some(statusTransitionModels),
                totalAmount = Some(BigDecimal(15)),
              )
              .create
            val orderItem =
              Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid), totalPriceAmount = Some(15)).create
            val paymenTransactionA = Factory.paymentTransaction(order).create
            val paymenTransactionB = Factory.paymentTransaction(order).create

            Post(
              s"/v1/orders.send_receipt?order_id=${order.id}&payment_transaction_id=${paymenTransactionA.id}",
              sendReceiptData,
            ).addHeader(authorizationHeader) ~> routes ~> check {
              assertCustomerMatchesReceiptRecipient(originalCustomer.id, sendReceiptData)
            }
          }

          "send the notification shoud not update the customer id on the order if that is already set (order assigned)" in new OrdersSendReceiptFSpecContext {
            val globalCustomer = Factory.globalCustomerWithEmail(email = None).create
            val assignedCustomer = Factory.customerMerchant(merchant, globalCustomer).create

            val receiptEmail = randomEmail
            val sendReceiptData = SendReceiptData(receiptEmail)
            val globalCustomer2 = Factory.globalCustomerWithEmail(email = Option(receiptEmail)).create
            val emailCustomer = Factory.customerMerchant(merchant, globalCustomer2).create

            val order = Factory
              .order(
                merchant,
                customer = Some(assignedCustomer),
                location = Some(rome),
                statusTransitions = Some(statusTransitionModels),
              )
              .create
            val orderItem = Factory.orderItem(order, paymentStatus = Some(PaymentStatus.Paid)).create
            val paymenTransactionA = Factory.paymentTransaction(order).create
            val paymenTransactionB = Factory.paymentTransaction(order).create

            Post(
              s"/v1/orders.send_receipt?order_id=${order.id}&payment_transaction_id=${paymenTransactionA.id}",
              sendReceiptData,
            ).addHeader(authorizationHeader) ~> routes ~> check {
              assertCustomerMatchesReceiptRecipient(emailCustomer.id, sendReceiptData)
              assertCustomerLinkedToOrder(assignedCustomer.id, order.id)
            }
          }
        }
      }

      "if the order doesn't exist" should {
        "return 404" in new OrdersSendReceiptFSpecContext {
          val sendReceiptData = random[SendReceiptData]
          Post(s"/v1/orders.send_receipt?order_id=${UUID.randomUUID}", sendReceiptData)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the order doesn't belong to the current merchant" should {
        "return 404" in new OrdersSendReceiptFSpecContext {
          val sendReceiptData = random[SendReceiptData]
          val competitor = Factory.merchant.create
          val order = Factory.order(competitor).create
          Post(s"/v1/orders.send_receipt?order_id=${order.id}", sendReceiptData)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
