package io.paytouch.core.resources.timeoffcards

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class TimeOffCardsListFSpec extends TimeOffCardsFSpec {

  abstract class TimeOffCardsListFSpecContext extends TimeOffCardResourceFSpecContext {
    val now = UtcTime.now
    val yesterday = now.minusDays(1)
    val nowParam = now.toLocalDateTime
    val yesterdayParam = yesterday.toLocalDateTime
  }

  "GET /v1/time_off_cards.list" in {
    "if request has valid token" in {

      "with no parameters" should {
        "return all the user time off cards" in new TimeOffCardsListFSpecContext {

          val timeOffCardA = Factory.timeOffCard(user).create
          val timeOffCardB = Factory.timeOffCard(user).create
          val timeOffCardC = Factory.timeOffCard(user).create

          Get(s"/v1/time_off_cards.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeOffCards = responseAs[PaginatedApiResponse[Seq[TimeOffCard]]].data
            timeOffCards.map(_.id) should containTheSameElementsAs(
              Seq(timeOffCardA.id, timeOffCardB.id, timeOffCardC.id),
            )
            assertResponse(timeOffCards.find(_.id == timeOffCardA.id).get, timeOffCardA)
            assertResponse(timeOffCards.find(_.id == timeOffCardB.id).get, timeOffCardB)
            assertResponse(timeOffCards.find(_.id == timeOffCardC.id).get, timeOffCardC)
          }
        }
      }

      "with filter location_id" should {
        "return all the user time off cards filtered by location id" in new TimeOffCardsListFSpecContext {
          val newYork = Factory.location(merchant).create
          val newYorkUser = Factory.user(merchant, locations = Seq(newYork)).create

          val onlyLondonUser = Factory.user(merchant, locations = Seq(london)).create

          val timeOffCardA = Factory.timeOffCard(newYorkUser).create
          val timeOffCardB = Factory.timeOffCard(onlyLondonUser).create
          val timeOffCardC = Factory.timeOffCard(user).create

          Get(s"/v1/time_off_cards.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeOffCards = responseAs[PaginatedApiResponse[Seq[TimeOffCard]]].data
            timeOffCards.map(_.id) ==== Seq(timeOffCardC.id)
            assertResponse(timeOffCards.find(_.id == timeOffCardC.id).get, timeOffCardC)
          }
        }
      }

      "with filter q" should {
        "return all the user time off cards filtered by q" in new TimeOffCardsListFSpecContext {
          val daniela = Factory.user(merchant, firstName = Some("Daniela"), locations = locations).create
          val danielaTimeOffCard = Factory.timeOffCard(daniela).create
          val timeOffCardA = Factory.timeOffCard(user).create
          val timeOffCardB = Factory.timeOffCard(user).create

          Get(s"/v1/time_off_cards.list?q=aniel").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeOffCards = responseAs[PaginatedApiResponse[Seq[TimeOffCard]]].data
            timeOffCards.map(_.id) ==== Seq(danielaTimeOffCard.id)
            assertResponse(timeOffCards.find(_.id == danielaTimeOffCard.id).get, danielaTimeOffCard)
          }
        }
      }

      "with filters from and to" should {
        "return all time off cards filtered by from and to dates" in new TimeOffCardsListFSpecContext {
          val timeCardA = Factory
            .timeOffCard(user, startAt = Some(yesterday.plusHours(6)), endAt = Some(now.minusHours(6)))
            .create
          val timeCardB =
            Factory.timeOffCard(user, startAt = Some(now.plusDays(6)), endAt = Some(now.plusDays(7))).create
          val timeCardC = Factory
            .timeOffCard(user, startAt = Some(yesterday.minusDays(7)), endAt = Some(yesterday.minusDays(6)))
            .create

          Get(s"/v1/time_off_cards.list?from=$yesterdayParam&to=$nowParam")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeOffCards = responseAs[PaginatedApiResponse[Seq[TimeOffCard]]].data
            timeOffCards.map(_.id) ==== Seq(timeCardA.id)
            assertResponse(timeOffCards.find(_.id == timeCardA.id).get, timeCardA)
          }
        }
      }
    }
  }
}
