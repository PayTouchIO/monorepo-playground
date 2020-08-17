package io.paytouch.core.resources.timecards

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class TimeCardsGetFSpec extends TimeCardsFSpec {

  abstract class TimeCardsGetFSpecContext extends TimeCardResourceFSpecContext

  "GET /v1/time_cards.get?time_card_id=$" in {
    "if request has valid token" in {

      "if the time card exists" should {

        "with no parameters" should {
          "return the time card" in new TimeCardsGetFSpecContext {
            val timeCardRecord = Factory.timeCard(user, london).create

            Get(s"/v1/time_cards.get?time_card_id=${timeCardRecord.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val timeCardEntity = responseAs[ApiResponse[TimeCard]].data
              assertResponse(timeCardEntity, timeCardRecord)
            }
          }
        }

        "with expand[]=shift" should {
          "return the time card with expanded shifts" in new TimeCardsGetFSpecContext {
            val shift = Factory.shift(user, london).create
            val timeCardRecord = Factory.timeCard(user, london, shift = Some(shift)).create

            Get(s"/v1/time_cards.get?time_card_id=${timeCardRecord.id}&expand[]=shift")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatusOK()

              val timeCardEntity = responseAs[ApiResponse[TimeCard]].data
              assertResponse(timeCardEntity, timeCardRecord, Some(shift))
            }
          }
        }
      }

      "if the time card does not belong to the merchant" should {
        "return 404" in new TimeCardsGetFSpecContext {
          val competitor = Factory.merchant.create
          val competitorLocation = Factory.location(competitor).create
          val competitorUser = Factory.user(competitor).create
          val timeCardCompetitor = Factory.timeCard(competitorUser, competitorLocation).create

          Get(s"/v1/time_cards.get?time_card_id=${timeCardCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the user does not has access to the location associated to the time card" should {
        "return 404" in new TimeCardsGetFSpecContext {
          val newYork = Factory.location(merchant).create
          val daniela = Factory.user(merchant).create
          val timeCardCompetitor = Factory.timeCard(daniela, newYork).create

          Get(s"/v1/time_cards.get?time_card_id=${timeCardCompetitor.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }

      "if the time card does not exist" should {
        "return 404" in new TimeCardsGetFSpecContext {
          Get(s"/v1/time_cards.get?time_card_id=${UUID.randomUUID}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
