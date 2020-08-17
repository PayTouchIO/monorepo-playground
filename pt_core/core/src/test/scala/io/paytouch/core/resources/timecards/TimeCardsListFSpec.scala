package io.paytouch.core.resources.timecards

import java.time.LocalDateTime

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class TimeCardsListFSpec extends TimeCardsFSpec {

  abstract class TimeCardsListFSpecContext extends TimeCardResourceFSpecContext {
    val now = UtcTime.now
    val yesterday = now.minusDays(1)
    val nowParam = now.toLocalDateTime
    val yesterdayParam = yesterday.toLocalDateTime
  }

  "GET /v1/time_cards.list" in {
    "if request has valid token" in {

      "with no parameters" should {
        "return all the time cards" in new TimeCardsListFSpecContext {
          val newYork = Factory.location(merchant).create

          val timeCardA = Factory.timeCard(user, london).create
          val timeCardB = Factory.timeCard(user, london).create
          val timeCardC = Factory.timeCard(user, rome).create

          val notAccessibleTimeCard = Factory.timeCard(user, newYork).create

          Get(s"/v1/time_cards.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeCards = responseAs[PaginatedApiResponse[Seq[TimeCard]]].data
            timeCards.map(_.id) should containTheSameElementsAs(Seq(timeCardA.id, timeCardB.id, timeCardC.id))
            assertResponse(timeCards.find(_.id == timeCardA.id).get, timeCardA)
            assertResponse(timeCards.find(_.id == timeCardB.id).get, timeCardB)
            assertResponse(timeCards.find(_.id == timeCardC.id).get, timeCardC)
          }
        }

        "return all the time cards associated to a deleted user" in new TimeCardsListFSpecContext {
          val newYork = Factory.location(merchant).create

          val deletedUser = Factory.user(merchant, deletedAt = Some(UtcTime.now)).create

          val timeCardA = Factory.timeCard(deletedUser, london).create
          val timeCardB = Factory.timeCard(deletedUser, london).create
          val timeCardC = Factory.timeCard(deletedUser, rome).create

          val notAccessibleTimeCard = Factory.timeCard(deletedUser, newYork).create

          Get(s"/v1/time_cards.list").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeCards = responseAs[PaginatedApiResponse[Seq[TimeCard]]].data
            timeCards.map(_.id) should containTheSameElementsAs(Seq(timeCardA.id, timeCardB.id, timeCardC.id))
            assertResponse(timeCards.find(_.id == timeCardA.id).get, timeCardA)
            assertResponse(timeCards.find(_.id == timeCardB.id).get, timeCardB)
            assertResponse(timeCards.find(_.id == timeCardC.id).get, timeCardC)
          }
        }
      }

      "with filter location_id" should {
        "return all the time cards filtered by location id" in new TimeCardsListFSpecContext {
          val timeCardA = Factory.timeCard(user, london).create
          val timeCardB = Factory.timeCard(user, london).create
          val timeCardC = Factory.timeCard(user, rome).create

          Get(s"/v1/time_cards.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeCards = responseAs[PaginatedApiResponse[Seq[TimeCard]]].data
            timeCards.map(_.id) ==== Seq(timeCardC.id)
            assertResponse(timeCards.find(_.id == timeCardC.id).get, timeCardC)
          }
        }
      }

      "with filter from" should {
        "return all the time cards filtered by from date" in new TimeCardsListFSpecContext {
          val timeCardA = Factory.timeCard(user, london, startAt = Some(now)).create
          val timeCardB = Factory.timeCard(user, london, startAt = Some(now)).create
          val timeCardC = Factory.timeCard(user, rome, startAt = Some(now.minusDays(7))).create

          Get(s"/v1/time_cards.list?from=$yesterdayParam").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeCards = responseAs[PaginatedApiResponse[Seq[TimeCard]]].data
            timeCards.map(_.id) should containTheSameElementsAs(Seq(timeCardA.id, timeCardB.id))
            assertResponse(timeCards.find(_.id == timeCardA.id).get, timeCardA)
            assertResponse(timeCards.find(_.id == timeCardB.id).get, timeCardB)
          }
        }
      }

      "with filter to" should {
        "return all the time cards filtered by to date" in new TimeCardsListFSpecContext {
          val timeCardA = Factory.timeCard(user, london, startAt = Some(now)).create
          val timeCardB = Factory.timeCard(user, london, startAt = Some(now)).create
          val timeCardC = Factory.timeCard(user, rome, startAt = Some(now.minusDays(7))).create

          Get(s"/v1/time_cards.list?to=$yesterdayParam").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeCards = responseAs[PaginatedApiResponse[Seq[TimeCard]]].data
            timeCards.map(_.id) ==== Seq(timeCardC.id)
            assertResponse(timeCards.find(_.id == timeCardC.id).get, timeCardC)
          }
        }
      }

      "with filters from and to" should {
        "return all the time cards filtered by from and to dates" in new TimeCardsListFSpecContext {
          val timeCardA = Factory
            .timeCard(user, london, startAt = Some(yesterday.plusHours(6)), endAt = Some(now.minusHours(6)))
            .create
          val timeCardB =
            Factory.timeCard(user, london, startAt = Some(now.plusDays(6)), endAt = Some(now.plusDays(7))).create
          val timeCardC = Factory
            .timeCard(user, rome, startAt = Some(yesterday.minusDays(7)), endAt = Some(yesterday.minusDays(6)))
            .create

          Get(s"/v1/time_cards.list?from=$yesterdayParam&to=$nowParam")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeCards = responseAs[PaginatedApiResponse[Seq[TimeCard]]].data
            timeCards.map(_.id) ==== Seq(timeCardA.id)
            assertResponse(timeCards.find(_.id == timeCardA.id).get, timeCardA)
          }
        }
      }

      "with filter status" should {
        "return all the time cards filtered by status open" in new TimeCardsListFSpecContext {
          val timeCardA = Factory.timeCard(user, london, endAt = None).create
          val timeCardB = Factory.timeCard(user, london, endAt = None).create
          val timeCardC = Factory.timeCard(user, rome).create

          Get(s"/v1/time_cards.list?status=open").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeCards = responseAs[PaginatedApiResponse[Seq[TimeCard]]].data
            timeCards.map(_.id) should containTheSameElementsAs(Seq(timeCardA.id, timeCardB.id))
            assertResponse(timeCards.find(_.id == timeCardA.id).get, timeCardA)
            assertResponse(timeCards.find(_.id == timeCardB.id).get, timeCardB)
          }
        }
        "return all the time cards filtered by status closed" in new TimeCardsListFSpecContext {
          val timeCardA = Factory.timeCard(user, london, endAt = None).create
          val timeCardB = Factory.timeCard(user, london, endAt = None).create
          val timeCardC = Factory.timeCard(user, rome).create

          Get(s"/v1/time_cards.list?status=closed").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeCards = responseAs[PaginatedApiResponse[Seq[TimeCard]]].data
            timeCards.map(_.id) ==== Seq(timeCardC.id)
            assertResponse(timeCards.find(_.id == timeCardC.id).get, timeCardC)
          }
        }
      }

      "with filter q" should {
        "return all the time cards filtered by given query" in new TimeCardsListFSpecContext {
          val daniela = Factory.user(merchant, firstName = Some("Daniela")).create
          val danielaTimeCard = Factory.timeCard(daniela, rome).create
          val timeCardA = Factory.timeCard(user, rome).create
          val timeCardB = Factory.timeCard(user, rome).create

          Get(s"/v1/time_cards.list?q=aniel").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeCards = responseAs[PaginatedApiResponse[Seq[TimeCard]]].data
            timeCards.map(_.id) ==== Seq(danielaTimeCard.id)
            assertResponse(timeCards.find(_.id == danielaTimeCard.id).get, danielaTimeCard)
          }
        }
      }

      "with expand[]=shift" should {
        "return all the time cards with shift" in new TimeCardsListFSpecContext {
          val shift = Factory.shift(user, london).create
          val timeCardA = Factory.timeCard(user, london, shift = Some(shift)).create
          val timeCardB = Factory.timeCard(user, london, shift = Some(shift)).create
          val timeCardC = Factory.timeCard(user, london, shift = Some(shift)).create

          Get(s"/v1/time_cards.list?expand[]=shift").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()

            val timeCards = responseAs[PaginatedApiResponse[Seq[TimeCard]]].data
            timeCards.map(_.id) should containTheSameElementsAs(Seq(timeCardA.id, timeCardB.id, timeCardC.id))
            assertResponse(timeCards.find(_.id == timeCardA.id).get, timeCardA, Some(shift))
            assertResponse(timeCards.find(_.id == timeCardB.id).get, timeCardB, Some(shift))
            assertResponse(timeCards.find(_.id == timeCardC.id).get, timeCardC, Some(shift))
          }
        }
      }
    }
  }
}
