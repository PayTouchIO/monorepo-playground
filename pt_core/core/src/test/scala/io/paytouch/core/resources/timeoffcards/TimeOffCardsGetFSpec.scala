package io.paytouch.core.resources.timeoffcards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TimeOffCardsGetFSpec extends TimeOffCardsFSpec {

  abstract class TimeOffCardsGetFSpecContext extends TimeOffCardResourceFSpecContext

  "GET /v1/time_off_cards.get?time_off_card_id=$" in {
    "if request has valid token" in {

      "if the time off card exists" should {

        "with no parameters" should {
          "return the time off card" in new TimeOffCardsGetFSpecContext {
            val timeOffCardRecord = Factory.timeOffCard(user).create

            Get(s"/v1/time_off_cards.get?time_off_card_id=${timeOffCardRecord.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val timeOffCardEntity = responseAs[ApiResponse[TimeOffCard]].data
              assertResponse(timeOffCardEntity, timeOffCardRecord)
            }
          }
        }
      }

      "if the time off card does not belong to the merchant" should {
        "return 404" in new TimeOffCardsGetFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(competitor).create
          val timeOffCardCompetitor = Factory.timeOffCard(competitorUser).create

          Get(s"/v1/time_off_cards.get?time_off_card_id=${timeOffCardCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the time off card does not exist" should {
        "return 404" in new TimeOffCardsGetFSpecContext {
          Get(s"/v1/time_off_cards.get?time_off_card_id=${UUID.randomUUID}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
