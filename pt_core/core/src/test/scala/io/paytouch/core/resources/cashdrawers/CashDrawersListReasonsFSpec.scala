package io.paytouch.core.resources.cashdrawers

import io.paytouch.core.entities.{ CashDrawer => CashDrawerEntity, _ }
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class CashDrawersListReasonsFSpec extends CashDrawersFSpec {

  "GET /v1/cash_drawers.list" in {
    "if request has valid token" in {

      "with no parameter" should {
        "return a paginated list of all cash drawers reasons" in new CashDrawerResourceFSpecContext {
          Get("/v1/cash_drawers.list_reasons").addHeader(authorizationHeader) ~> routes ~> check {
            assertStatusOK()
            val cashDrawerReasons = responseAs[ApiResponse[Seq[CashDrawerReason]]]
            cashDrawerReasons.data.length must beGreaterThan(0)
          }
        }
      }
    }
  }

}
