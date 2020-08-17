package io.paytouch.core.resources.transferorders

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ TransferOrder => TransferOrderEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TransferOrdersGetFSpec extends TransferOrdersFSpec {

  abstract class TransferOrderGetFSpecContext extends TransferOrderResourceFSpecContext

  "GET /v1/transfer_orders.get?transfer_order_id=$" in {
    "if request has valid token" in {

      "if the transfer order belongs to the merchant" in {
        "with no parameters" should {
          "return a transfer order" in new TransferOrderGetFSpecContext {
            val transferOrder = Factory.transferOrder(rome, london, user).create

            Get(s"/v1/transfer_orders.get?transfer_order_id=${transferOrder.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[TransferOrderEntity]].data
              assertResponse(entity, transferOrder)
            }
          }
        }

        "with expand[]=from_location" should {
          "return a transfer order" in new TransferOrderGetFSpecContext {
            val transferOrder = Factory.transferOrder(rome, london, user).create

            Get(s"/v1/transfer_orders.get?transfer_order_id=${transferOrder.id}&expand[]=from_location")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[TransferOrderEntity]].data
              assertResponse(entity, transferOrder, fromLocation = Some(rome))
            }
          }
        }

        "with expand[]=to_location" should {
          "return a transfer order" in new TransferOrderGetFSpecContext {
            val transferOrder = Factory.transferOrder(rome, london, user).create

            Get(s"/v1/transfer_orders.get?transfer_order_id=${transferOrder.id}&expand[]=to_location")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[TransferOrderEntity]].data
              assertResponse(entity, transferOrder, toLocation = Some(london))
            }
          }
        }

        "with expand[]=user" should {
          "return a transfer order" in new TransferOrderGetFSpecContext {
            val transferOrder = Factory.transferOrder(rome, london, user).create

            Get(s"/v1/transfer_orders.get?transfer_order_id=${transferOrder.id}&expand[]=user")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[TransferOrderEntity]].data
              assertResponse(entity, transferOrder, user = Some(user))
            }
          }
        }
      }

      "if the transfer order does not belong to the merchant" should {
        "return 404" in new TransferOrderGetFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(competitor).create
          val competitorFromLocation = Factory.location(competitor).create
          val competitorToLocation = Factory.location(competitor).create
          val competitorTransferOrder =
            Factory.transferOrder(competitorFromLocation, competitorToLocation, competitorUser).create

          Get(s"/v1/transfer_orders.get?transfer_order_id=${competitorTransferOrder.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
