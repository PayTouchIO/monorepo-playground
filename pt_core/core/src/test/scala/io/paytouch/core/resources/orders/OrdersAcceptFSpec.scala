package io.paytouch.core.resources.orders

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.data.model.OrderRecord
import io.paytouch.core.data.model.enums.AcceptanceStatus
import io.paytouch.core.entities.{ ApiResponse, LoyaltyPoints, Order, OrderAcception }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory, ZonedDateTimeAssertions, UtcTime }
import io.paytouch.core.utils.TimeImplicits._

import scala.concurrent.duration._

class OrdersAcceptFSpec extends OrdersFSpec with ZonedDateTimeAssertions {
  val onlineOrderAttributeDao = daos.onlineOrderAttributeDao

  class OrdersAcceptFSpecContext extends OrderResourceFSpecContext {
    def assertAccepted(
        order: OrderRecord,
        estimatedPrepTimeInMins: Option[Int] = None,
        estimatedDrivingTimeInMins: Option[Int] = None,
      ) = {
      val onlineOrderAttribute = onlineOrderAttributeDao.findByOrderId(order.id).await.get
      onlineOrderAttribute.acceptanceStatus ==== AcceptanceStatus.Accepted
      onlineOrderAttribute.acceptedAt.get must beWithin(5.seconds, UtcTime.now)

      onlineOrderAttribute.rejectionReason ==== None
      onlineOrderAttribute.rejectedAt ==== None

      if (estimatedPrepTimeInMins.isDefined) {
        onlineOrderAttribute.estimatedPrepTimeInMins ==== estimatedPrepTimeInMins
        onlineOrderAttribute.estimatedReadyAt must beSome
      }
      else {
        onlineOrderAttribute.estimatedPrepTimeInMins ==== None
        onlineOrderAttribute.estimatedReadyAt ==== None
      }

      if (estimatedPrepTimeInMins.isDefined && estimatedDrivingTimeInMins.isDefined)
        onlineOrderAttribute.estimatedDeliveredAt must beSome
      else
        onlineOrderAttribute.estimatedDeliveredAt ==== None
    }
  }

  "POST /v1/orders.accept?order_id=$" in {
    "if request has valid token" in {

      "if the order exists and is an online order" in {
        "if it's in status pending or accepted" should {
          "set the order as accepted" in new OrdersAcceptFSpecContext {
            val onlineOrderAttribute =
              Factory.onlineOrderAttribute(merchant, acceptanceStatus = Some(AcceptanceStatus.Pending)).create
            val order = Factory
              .order(merchant, location = Some(london), onlineOrderAttribute = Some(onlineOrderAttribute))
              .create

            Post(s"/v1/orders.accept?order_id=${order.id}").addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderEntity = responseAs[ApiResponse[Order]].data

              val reloadedOrder = orderDao.findById(order.id).await.get
              assertAccepted(reloadedOrder)
              assertFullyExpandedResponse(order, orderEntity)
            }
          }

          "set the order as accepted with estimated prep time" in new OrdersAcceptFSpecContext {
            val orderAcception = OrderAcception(
              estimatedPrepTimeInMins = Some(25),
            )

            val onlineOrderAttribute =
              Factory.onlineOrderAttribute(merchant, acceptanceStatus = Some(AcceptanceStatus.Pending)).create
            val order = Factory
              .order(merchant, location = Some(london), onlineOrderAttribute = Some(onlineOrderAttribute))
              .create

            Post(s"/v1/orders.accept?order_id=${order.id}", orderAcception)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderEntity = responseAs[ApiResponse[Order]].data

              val reloadedOrder = orderDao.findById(order.id).await.get
              assertAccepted(reloadedOrder, orderAcception.estimatedPrepTimeInMins)
              assertFullyExpandedResponse(order, orderEntity)
            }
          }

          "set the order as accepted with estimated prep time and estimated driving time" in new OrdersAcceptFSpecContext {
            val orderAcception = OrderAcception(
              estimatedPrepTimeInMins = Some(45),
            )

            val onlineOrderAttribute =
              Factory.onlineOrderAttribute(merchant, acceptanceStatus = Some(AcceptanceStatus.Pending)).create
            val address = Factory.orderDeliveryAddress(merchant, estimatedDrivingTimeInMins = Some(10)).create
            val order = Factory
              .order(
                merchant,
                location = Some(london),
                onlineOrderAttribute = Some(onlineOrderAttribute),
                orderDeliveryAddress = Some(address),
              )
              .create

            Post(s"/v1/orders.accept?order_id=${order.id}", orderAcception)
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()
              val orderEntity = responseAs[ApiResponse[Order]].data

              val reloadedOrder = orderDao.findById(order.id).await.get
              assertAccepted(reloadedOrder, orderAcception.estimatedPrepTimeInMins, address.estimatedDrivingTimeInMins)
              assertFullyExpandedResponse(order, orderEntity)
            }
          }
        }

        "if it's in status rejected" should {
          "return 400" in new OrdersAcceptFSpecContext {
            val onlineOrderAttribute =
              Factory.onlineOrderAttribute(merchant, acceptanceStatus = Some(AcceptanceStatus.Rejected)).create
            val order = Factory
              .order(merchant, location = Some(london), onlineOrderAttribute = Some(onlineOrderAttribute))
              .create

            Post(s"/v1/orders.accept?order_id=${order.id}").addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.BadRequest)
              assertErrorCode("InvalidAcceptanceStatusChange")
            }
          }
        }
      }

      "if the order exists and is NOT an online order" should {
        "return 400" in new OrdersAcceptFSpecContext {
          val order = Factory.order(merchant, location = Some(london)).create

          Post(s"/v1/orders.accept?order_id=${order.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("NonOnlineOrder")
          }
        }
      }

      "if the order doesn't exist" should {
        "return 400" in new OrdersAcceptFSpecContext {
          Post(s"/v1/orders.accept?order_id=${UUID.randomUUID}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("NonAccessibleOrderIds", "NonOnlineOrder")
          }
        }
      }

      "if the order belongs to another merchant" should {
        "return 400" in new OrdersAcceptFSpecContext {
          val competitor = Factory.merchant.create
          val onlineOrderAttribute =
            Factory.onlineOrderAttribute(competitor, acceptanceStatus = Some(AcceptanceStatus.Pending)).create
          val competitorOrder = Factory.order(competitor, onlineOrderAttribute = Some(onlineOrderAttribute)).create
          Post(s"/v1/orders.accept?order_id=${competitorOrder.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
          }
        }
      }
    }
  }

}
