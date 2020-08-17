package io.paytouch.core.resources.timeoffcards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.UtcTime

class TimeOffCardsCreateFSpec extends TimeOffCardsFSpec {

  abstract class TimeOffCardsCreateFSpecContext extends TimeOffCardResourceFSpecContext

  "POST /v1/time_off_cards.create" in {
    "if request has valid token" in {

      "create time off card not associated to any shift and return 201" in new TimeOffCardsCreateFSpecContext {
        val newTimeOffCardId = UUID.randomUUID
        val endAt = UtcTime.now
        val creation = random[TimeOffCardCreation].copy(userId = user.id)

        Post(s"/v1/time_off_cards.create?time_off_card_id=$newTimeOffCardId", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusCreated()

          val timeOffCard = responseAs[ApiResponse[TimeOffCard]].data
          assertCreation(newTimeOffCardId, creation)
          assertResponseById(newTimeOffCardId, timeOffCard)
        }
      }
    }

    "if request has invalid user id" should {
      "return 404" in new TimeOffCardsCreateFSpecContext {
        val randomUuid = UUID.randomUUID
        val creation = random[TimeOffCardCreation].copy(userId = UUID.randomUUID)
        Post(s"/v1/time_off_cards.create?time_off_card_id=$randomUuid", creation)
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid token" should {
      "be rejected" in new TimeOffCardsCreateFSpecContext {
        val randomUuid = UUID.randomUUID
        val creation = random[TimeOffCardCreation]
        Post(s"/v1/time_off_cards.create?time_off_card_id=$randomUuid", creation)
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
