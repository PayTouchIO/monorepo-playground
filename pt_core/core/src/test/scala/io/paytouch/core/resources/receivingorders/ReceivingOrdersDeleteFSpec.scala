package io.paytouch.core.resources.receivingorders

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class ReceivingOrdersDeleteFSpec extends ReceivingOrdersFSpec {

  "POST /v1/receiving_orders.delete" in {
    "if request has valid token" in {
      "if receiving order belongs to the current merchant" should {
        "delete a receiving order" in new ReceivingOrderResourceFSpecContext {
          val receivingOrder = Factory.receivingOrder(rome, user).create

          Post(s"/v1/receiving_orders.delete", Ids(ids = Seq(receivingOrder.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            receivingOrderDao.findById(receivingOrder.id).await should beEmpty
          }
        }
      }

      "if receiving order does not belongs to the current merchant" should {
        "NOT delete a receiving order" in new ReceivingOrderResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create
          val competitorUser = Factory.user(competitor).create
          val competitorReceivingOrder = Factory.receivingOrder(competitorLocation, competitorUser).create

          Post(s"/v1/receiving_orders.delete", Ids(ids = Seq(competitorReceivingOrder.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            receivingOrderDao.findById(competitorReceivingOrder.id).await should beSome
          }
        }
      }

    }
  }
}
