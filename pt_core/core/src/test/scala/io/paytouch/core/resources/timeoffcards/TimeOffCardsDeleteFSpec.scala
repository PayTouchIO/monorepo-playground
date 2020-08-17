package io.paytouch.core.resources.timeoffcards

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TimeOffCardsDeleteFSpec extends TimeOffCardsFSpec {
  "POST /v1/time_off_cards.delete" in {
    "if request has valid token" in {
      "if time off card belongs to a location accessible to the current user" should {
        "delete a time off cards" in new TimeOffCardResourceFSpecContext {
          val timeOffCard = Factory.timeOffCard(user).create

          Post(s"/v1/time_off_cards.delete", Ids(ids = Seq(timeOffCard.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            timeOffCardDao.findById(timeOffCard.id).await should beEmpty
          }
        }
      }
    }
  }
}
