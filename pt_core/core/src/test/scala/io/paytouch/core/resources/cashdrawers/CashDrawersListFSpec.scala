package io.paytouch.core.resources.cashdrawers

import java.time.ZonedDateTime

import io.paytouch.core.entities.{ CashDrawer => CashDrawerEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CashDrawersListFSpec extends CashDrawersFSpec {

  "GET /v1/cash_drawers.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all cash drawers of users in locations accessible to the current user" in new CashDrawerResourceFSpecContext {
          val newYork = Factory.location(merchant).create
          val userInNewYork = Factory.user(merchant, locations = Seq(newYork)).create

          val cashDrawer1 = Factory.cashDrawer(user, rome).create
          val cashDrawer2 = Factory.cashDrawer(user, rome).create
          val cashDrawer3 = Factory.cashDrawer(user, rome).create
          val cashDrawer4 = Factory.cashDrawer(userInNewYork, newYork).create

          Get("/v1/cash_drawers.list").addHeader(authorizationHeader) ~> routes ~> check {
            val cashDrawers = responseAs[PaginatedApiResponse[Seq[CashDrawerEntity]]]
            cashDrawers.data.map(_.id) ==== Seq(cashDrawer1.id, cashDrawer2.id, cashDrawer3.id)
            assertResponse(cashDrawer1, cashDrawers.data.find(_.id == cashDrawer1.id).get)
            assertResponse(cashDrawer2, cashDrawers.data.find(_.id == cashDrawer2.id).get)
            assertResponse(cashDrawer3, cashDrawers.data.find(_.id == cashDrawer3.id).get)
          }
        }
      }

      "with location_id parameter" should {
        "return a paginated list of all cash drawers filtered by cash drawers belonging to the given location" in new CashDrawerResourceFSpecContext {
          val userInRome = Factory.user(merchant, locations = Seq(rome)).create
          val userInLondon = Factory.user(merchant, locations = Seq(london)).create
          val cashDrawer1 = Factory.cashDrawer(user, rome).create
          val cashDrawer2 = Factory.cashDrawer(userInRome, rome).create
          val cashDrawer3 = Factory.cashDrawer(userInLondon, london).create

          Get(s"/v1/cash_drawers.list?location_id=${rome.id}").addHeader(authorizationHeader) ~> routes ~> check {
            val cashDrawers = responseAs[PaginatedApiResponse[Seq[CashDrawerEntity]]]
            cashDrawers.data.map(_.id) ==== Seq(cashDrawer1.id, cashDrawer2.id)
            assertResponse(cashDrawer1, cashDrawers.data.find(_.id == cashDrawer1.id).get)
            assertResponse(cashDrawer2, cashDrawers.data.find(_.id == cashDrawer2.id).get)
          }
        }
      }

      "with updated_since parameter" should {
        "return a paginated list of all cash drawers filtered by cash drawers updated since the given time" in new CashDrawerResourceFSpecContext {
          val now = ZonedDateTime.parse("2015-12-03T20:15:30Z")

          val cashDrawer1 = Factory.cashDrawer(user, rome, overrideNow = Some(now.minusDays(1))).create
          val cashDrawer2 = Factory.cashDrawer(user, rome, overrideNow = Some(now)).create
          val cashDrawer3 = Factory.cashDrawer(user, rome, overrideNow = Some(now.plusDays(1))).create

          Get("/v1/cash_drawers.list?updated_since=2015-12-03").addHeader(authorizationHeader) ~> routes ~> check {
            val cashDrawers = responseAs[PaginatedApiResponse[Seq[CashDrawerEntity]]]
            cashDrawers.data.length ==== 2
            cashDrawers.data.map(_.id) ==== Seq(cashDrawer2.id, cashDrawer3.id)
            assertResponse(cashDrawer2, cashDrawers.data.find(_.id == cashDrawer2.id).get)
            assertResponse(cashDrawer3, cashDrawers.data.find(_.id == cashDrawer3.id).get)
          }
        }
      }
    }
  }

}
