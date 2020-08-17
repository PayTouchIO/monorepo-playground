package io.paytouch.core.resources.timecards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.TimeCardRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils.UtcTime
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TimeCardsClockFSpec extends TimeCardsFSpec {

  abstract class TimeCardsClockFSpecContext extends TimeCardResourceFSpecContext {
    lazy val inputPin = userPin
    lazy val inputLocationId = rome.id
    lazy val clockData = TimeCardClock(pin = inputPin, locationId = inputLocationId)

    def assertTimeCardWasClosed(timeCard: TimeCardRecord) = {
      val dbTimeCard = timeCardDao.findById(timeCard.id).await.get
      dbTimeCard.totalMins.get must beGreaterThan(0)
      dbTimeCard.endAt must beSome
    }

    def assertTimeCardIsOpen(timeCardEntity: TimeCard) = {
      val dbTimeCard = timeCardDao.findById(timeCardEntity.id).await.get
      dbTimeCard.endAt must beNone
    }
  }

  "POST /v1/time_cards.clock" in {
    "if request has valid token" in {
      "if there's an open timecard" should {
        "close it and return 200" in new TimeCardsClockFSpecContext {
          val timeCard = Factory
            .timeCard(user, rome, startAt = Some(UtcTime.now.minusMinutes(30)), endAt = None, totalMins = 0)
            .create

          Post(s"/v1/time_cards.clock", clockData)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertTimeCardWasClosed(timeCard)
          }
        }
      }
      "if there are no open timecards" should {
        "create it and return 201" in new TimeCardsClockFSpecContext {
          Post(s"/v1/time_cards.clock", clockData)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusCreated()
            val timeCardResponse = responseAs[ApiResponse[TimeCard]].data
            assertTimeCardIsOpen(timeCardResponse)
          }
        }
      }

      "if request has unmatching pin" should {
        "return 404" in new TimeCardsClockFSpecContext {
          override lazy val inputPin = "1111"
          Post(s"/v1/time_cards.clock", clockData)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("NoUserMatchingPin")
          }
        }
      }

      "if request has invalid location id" should {
        "return 404" in new TimeCardsClockFSpecContext {
          override lazy val inputLocationId = UUID.randomUUID
          Post(s"/v1/time_cards.clock", clockData)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCode("UserNotEnabledInLocation")
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new TimeCardsClockFSpecContext {
        Post(s"/v1/time_cards.clock", clockData)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
