package io.paytouch.core.resources.orders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.OrderRecord
import io.paytouch.core.data.model.enums.{ AcceptanceStatus, OrderStatus, PaymentStatus }
import io.paytouch.core.entities.{ ApiResponse, Order, OrderRejection }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, ZonedDateTimeAssertions, UtcTime }

import scala.concurrent.duration._

class OrdersRejectFSpec extends OrdersFSpec {
  val onlineOrderAttributeDao = daos.onlineOrderAttributeDao

  class OrdersRejectFSpecContext extends OrderResourceFSpecContext with ZonedDateTimeAssertions {
    lazy val rejectionReason = Some(randomWords)
    lazy val orderRejection = OrderRejection(rejectionReason)

    def assertRejected(order: OrderRecord, rejectionReason: Option[String]) = {
      order.paymentStatus ==== Some(PaymentStatus.Voided)
      order.status ==== Some(OrderStatus.Canceled)

      val onlineOrderAttribute = onlineOrderAttributeDao.findByOrderId(order.id).await.get
      onlineOrderAttribute.acceptanceStatus ==== AcceptanceStatus.Rejected
      onlineOrderAttribute.rejectionReason ==== rejectionReason
      onlineOrderAttribute.rejectedAt.get must beWithin(5.seconds, UtcTime.now)

      onlineOrderAttribute.acceptedAt ==== None
      onlineOrderAttribute.estimatedReadyAt ==== None
      onlineOrderAttribute.estimatedDeliveredAt ==== None
    }
  }

  "POST /v1/orders.reject?order_id=$" in {
    "if request has valid token" in {

      "if the order exists and is an online order" in {
        "if it's in status pending or rejected" should {
          "set the order as rejected" in new OrdersRejectFSpecContext {
            val onlineOrderAttribute =
              Factory.onlineOrderAttribute(merchant, acceptanceStatus = Some(AcceptanceStatus.Pending)).create
            val order = Factory
              .order(merchant, location = Some(london), onlineOrderAttribute = Some(onlineOrderAttribute))
              .create

            Post(s"/v1/orders.reject?order_id=${order.id}", orderRejection)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderEntity = responseAs[ApiResponse[Order]].data

              val reloadedOrder = orderDao.findById(order.id).await.get

              assertRejected(reloadedOrder, rejectionReason)
              assertFullyExpandedResponse(reloadedOrder, orderEntity)
            }
          }

          "set the order as rejected with a long reason" in new OrdersRejectFSpecContext {
            val onlineOrderAttribute =
              Factory.onlineOrderAttribute(merchant, acceptanceStatus = Some(AcceptanceStatus.Pending)).create
            val order = Factory
              .order(merchant, location = Some(london), onlineOrderAttribute = Some(onlineOrderAttribute))
              .create

            override lazy val rejectionReason = Some(randomText)

            Post(s"/v1/orders.reject?order_id=${order.id}", orderRejection)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderEntity = responseAs[ApiResponse[Order]].data

              val reloadedOrder = orderDao.findById(order.id).await.get

              assertRejected(reloadedOrder, rejectionReason)
              assertFullyExpandedResponse(reloadedOrder, orderEntity)
            }
          }
        }

        "if it's in status accepted" should {
          "return 400" in new OrdersRejectFSpecContext {
            val onlineOrderAttribute =
              Factory.onlineOrderAttribute(merchant, acceptanceStatus = Some(AcceptanceStatus.Accepted)).create
            val order = Factory
              .order(merchant, location = Some(london), onlineOrderAttribute = Some(onlineOrderAttribute))
              .create

            Post(s"/v1/orders.reject?order_id=${order.id}", orderRejection)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("InvalidAcceptanceStatusChange")
            }
          }
        }
      }

      "if the order exists and is NOT an online order" should {
        "return 400" in new OrdersRejectFSpecContext {
          val order = Factory.order(merchant, location = Some(london)).create

          Post(s"/v1/orders.reject?order_id=${order.id}", orderRejection)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("NonOnlineOrder")
          }
        }
      }

      "if the order doesn't exist" should {
        "return 400" in new OrdersRejectFSpecContext {
          Post(s"/v1/orders.reject?order_id=${UUID.randomUUID}", orderRejection)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("NonAccessibleOrderIds", "NonOnlineOrder")
          }
        }
      }

      "if the order belongs to another merchant" should {
        "return 400" in new OrdersRejectFSpecContext {
          val competitor = Factory.merchant.create
          val onlineOrderAttribute =
            Factory.onlineOrderAttribute(competitor, acceptanceStatus = Some(AcceptanceStatus.Pending)).create
          val competitorOrder = Factory.order(competitor, onlineOrderAttribute = Some(onlineOrderAttribute)).create
          Post(s"/v1/orders.reject?order_id=${competitorOrder.id}", orderRejection)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }
    }
  }

}
