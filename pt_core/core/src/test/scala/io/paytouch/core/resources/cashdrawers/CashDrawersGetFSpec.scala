package io.paytouch.core.resources.cashdrawers

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ CashDrawer => CashDrawerEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CashDrawersGetFSpec extends CashDrawersFSpec {

  "GET /v1/cash_drawers.get?cash_drawer_id=<cash-drawer-id>" in {
    "if request has valid token" in {

      "if the cash drawer belongs to the merchant" in {
        "if the cash drawer belongs to a location accessible to the current user" should {
          "return a cash drawer" in new CashDrawerResourceFSpecContext {
            val cashDrawer = Factory.cashDrawer(user, rome).create

            Get(s"/v1/cash_drawers.get?cash_drawer_id=${cashDrawer.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              val entity = responseAs[ApiResponse[CashDrawerEntity]].data
              assertResponse(cashDrawer, entity)
            }
          }
        }

        "if the cash drawer doesn't belong to a location accessible to the current user" should {
          "return 404" in new CashDrawerResourceFSpecContext {
            val newYork = Factory.location(merchant).create
            val newYorkUser = Factory.user(merchant, locations = Seq(newYork)).create
            val unaccessibleCashDrawer = Factory.cashDrawer(newYorkUser, newYork).create

            Get(s"/v1/cash_drawers.get?cash_drawer_id=${unaccessibleCashDrawer.id}")
              .addHeader(authorizationHeader) ~> routes ~> check {
              assertStatus(StatusCodes.NotFound)
            }
          }
        }
      }

      "if the cash drawer does not belong to the merchant" should {
        "return 404" in new CashDrawerResourceFSpecContext {
          val competitor = Factory.merchant.create
          val competitorUser = Factory.user(competitor).create
          val competitorLocation = Factory.location(competitor).create
          val competitorCashDrawer = Factory.cashDrawer(competitorUser, competitorLocation).create

          Get(s"/v1/cash_drawers.get?cash_drawer_id=${competitorCashDrawer.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
