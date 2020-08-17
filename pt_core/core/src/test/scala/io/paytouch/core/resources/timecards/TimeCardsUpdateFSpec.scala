package io.paytouch.core.resources.timecards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.TimeCardRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }
import io.paytouch.core.utils.UtcTime

class TimeCardsUpdateFSpec extends TimeCardsFSpec {

  abstract class TimeCardsUpdateFSpecContext extends TimeCardResourceFSpecContext {
    def assertTimeCardWasntChanged(originalTimeCard: TimeCardRecord) = {
      val dbTimeCard = timeCardDao.findById(originalTimeCard.id).await.get
      dbTimeCard ==== originalTimeCard
    }
  }

  "POST /v1/time_cards.update?time_card_id=$" in {
    "if request has valid token" in {
      "if time card belong to same merchant" should {
        "update time card and return 200" in new TimeCardsUpdateFSpecContext {
          val userInSameLocation = Factory.user(merchant, locations = Seq(rome)).create
          val timeCard = Factory.timeCard(user, rome, startAt = None, endAt = None).create
          val shift = Factory.shift(user, rome).create
          val update = random[TimeCardUpdate].copy(
            userId = Some(userInSameLocation.id),
            locationId = Some(rome.id),
            shiftId = Some(shift.id),
            startAt = None,
            endAt = None,
          )

          Post(s"/v1/time_cards.update?time_card_id=${timeCard.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(timeCard.id, update, deltaMins = timeCard.deltaMins, timeCard.totalMins)
          }
        }
      }
      "if time card doesn't belong to a location accessible by the current user" should {
        "not update time card and return 404" in new TimeCardsUpdateFSpecContext {
          val newYork = Factory.location(merchant).create
          val userInNewYork = Factory.user(merchant, locations = Seq(newYork)).create
          val timeCard = Factory.timeCard(userInNewYork, newYork).create
          val update =
            random[TimeCardUpdate].copy(userId = Some(userInNewYork.id), locationId = Some(rome.id), startAt = None)
          Post(s"/v1/time_cards.update?time_card_id=${timeCard.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)

            assertTimeCardWasntChanged(timeCard)
          }
        }
      }
      "if time card doesn't belong to current user's merchant" in {
        "not update time card and return 404" in new TimeCardsUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(competitor).create
          val competitorLocation = Factory.location(competitor).create
          val competitorTimeCard = Factory.timeCard(competitorUser, competitorLocation).create

          val update =
            random[TimeCardUpdate].copy(
              userId = Some(competitorUser.id),
              locationId = Some(competitorLocation.id),
              startAt = None,
            )

          Post(s"/v1/time_cards.update?time_card_id=${competitorTimeCard.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertTimeCardWasntChanged(competitorTimeCard)
          }
        }
      }

      "if time card starts in the future" should {
        "not update time card and return 404" in new TimeCardsUpdateFSpecContext {
          val timeCard = Factory.timeCard(user, rome, startAt = None, endAt = None).create
          val startAt = UtcTime.now.plusHours(3)
          val endAt = startAt.plusHours(3)
          val update =
            random[TimeCardUpdate].copy(startAt = Some(startAt), endAt = Some(endAt))

          Post(s"/v1/time_cards.update?time_card_id=${timeCard.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.BadRequest)
            assertErrorCodesAtLeastOnce("InvalidFutureTime")
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new TimeCardsUpdateFSpecContext {
        val timeCardId = UUID.randomUUID
        val update = random[TimeCardUpdate].copy(startAt = None)
        Post(s"/v1/time_cards.update?time_card_id=$timeCardId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
