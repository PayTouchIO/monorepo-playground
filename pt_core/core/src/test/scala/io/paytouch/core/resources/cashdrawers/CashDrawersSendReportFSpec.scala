package io.paytouch.core.resources.cashdrawers

import java.time.ZoneId
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities.{ CashDrawer => CashDrawerEntity, _ }
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class CashDrawersSendReportFSpec extends CashDrawersFSpec {

  abstract class CashDrawersSendReportFSpecContext extends CashDrawerResourceFSpecContext

  "POST /v1/cash_drawers.send_report?cash_drawer_id=$" in {
    "if request has valid token" in {

      "if the cash drawer exists" should {
        "send the report" in new CashDrawersSendReportFSpecContext {
          val cashDrawer = Factory.cashDrawer(user, rome).create

          Post(s"/v1/cash_drawers.send_report?cash_drawer_id=${cashDrawer.id}")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NoContent)
          }
        }
      }

      "if the cash drawer doesn't exist" should {
        "return 404" in new CashDrawersSendReportFSpecContext {
          val cashDrawerId = UUID.randomUUID

          Post(s"/v1/cash_drawers.send_report?cash_drawer_id=$cashDrawerId")
            .addHeader(authorizationHeader) ~> routes ~> check {
            assertStatus(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
