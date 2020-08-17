package io.paytouch.core.resources.timeoffcards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.data.model.TimeOffCardRecord
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class TimeOffCardsUpdateFSpec extends TimeOffCardsFSpec {

  abstract class TimeOffCardsUpdateFSpecContext extends TimeOffCardResourceFSpecContext {
    def assertTimeOffCardWasntChanged(originalTimeOffCard: TimeOffCardRecord) = {
      val dbTimeOffCard = timeOffCardDao.findById(originalTimeOffCard.id).await.get
      dbTimeOffCard ==== originalTimeOffCard
    }
  }

  "POST /v1/time_off_cards.update?time_off_card_id=$" in {
    "if request has valid token" in {
      "if time off card belong to same merchant" should {
        "update time off card and return 200" in new TimeOffCardsUpdateFSpecContext {
          val timeOffCard = Factory.timeOffCard(user).create
          val update = random[TimeOffCardUpdate].copy(
            userId = Some(user.id),
            startAt = Some(UtcTime.now),
            endAt = Some(UtcTime.now),
          )

          Post(s"/v1/time_off_cards.update?time_off_card_id=${timeOffCard.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            assertUpdate(timeOffCard.id, update)
          }
        }
      }
      "if time off card doesn't belong to current user's merchant" in {
        "not update time off card and return 404" in new TimeOffCardsUpdateFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(competitor).create
          val competitorTimeOffCard = Factory.timeOffCard(competitorUser).create

          val update =
            random[TimeOffCardUpdate].copy(userId = Some(competitorUser.id))

          Post(s"/v1/time_off_cards.update?time_off_card_id=${competitorTimeOffCard.id}", update)
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
            assertTimeOffCardWasntChanged(competitorTimeOffCard)
          }
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new TimeOffCardsUpdateFSpecContext {
        val timeOffCardId = UUID.randomUUID
        val update = random[TimeOffCardUpdate]
        Post(s"/v1/time_off_cards.update?time_off_card_id=$timeOffCardId", update)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
