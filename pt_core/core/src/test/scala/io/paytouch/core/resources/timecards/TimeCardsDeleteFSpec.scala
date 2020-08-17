package io.paytouch.core.resources.timecards

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TimeCardsDeleteFSpec extends TimeCardsFSpec {

  "POST /v1/time_cards.delete" in {
    "if request has valid token" in {
      "if time card user belongs to a location accessible to the current user" should {
        "delete a time cards" in new TimeCardResourceFSpecContext {
          val timeCard = Factory.timeCard(user, london).create

          Post(s"/v1/time_cards.delete", Ids(ids = Seq(timeCard.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            timeCardDao.findById(timeCard.id).await should beEmpty
          }
        }
      }

      "if time card belongs to a location NOT accessible to the current user" should {
        "delete a time cards" in new TimeCardResourceFSpecContext {
          val newYork = Factory.location(merchant).create
          val otherUser = Factory.user(merchant, locations = Seq(newYork)).create
          val timeCard = Factory.timeCard(otherUser, newYork).create

          Post(s"/v1/time_cards.delete", Ids(ids = Seq(timeCard.id)))
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)

            timeCardDao.findById(timeCard.id).await should beSome
          }
        }
      }

    }
  }
}
