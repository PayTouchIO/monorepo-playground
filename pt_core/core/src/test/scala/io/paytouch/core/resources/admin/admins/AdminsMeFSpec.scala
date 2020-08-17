package io.paytouch.core.resources.admin.admins

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._

class AdminsMeFSpec extends AdminsFSpec {

  abstract class AdminsMeFSpecContext extends AdminResourceFSpecContext

  "GET /v1/admin/admins.me" in {
    "if request has valid token" in {

      "return the admin information without a password and with permissions" in new AdminsMeFSpecContext {

        Get("/v1/admin/admins.me").addHeader(adminAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val adminEntity = responseAs[ApiResponse[Admin]].data
          assertResponse(adminEntity, admin)
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new AdminsMeFSpecContext {

        Get(s"/v1/admin/admins.me").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
