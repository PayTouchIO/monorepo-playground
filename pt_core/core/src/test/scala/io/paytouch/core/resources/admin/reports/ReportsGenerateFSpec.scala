package io.paytouch.core.resources.admin.reports

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._

class ReportsGenerateFSpec extends ReportsFSpec {

  abstract class ReportsGenerateFSpecContext extends ReportsFSpecContext {
    val merchantId = UUID.randomUUID
  }

  "POST /v1/admin/reports.generate" in {
    "if request has valid token" in {

      "trigger computation of reports" in new ReportsGenerateFSpecContext {

        Post(s"/v1/admin/reports.generate?merchant_id[]=$merchantId")
          .addHeader(adminAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()

          responseAs[ApiResponse[Int]].data
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new ReportsGenerateFSpecContext {

        Post(s"/v1/admin/reports.generate?merchant_id[]=$merchantId")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
