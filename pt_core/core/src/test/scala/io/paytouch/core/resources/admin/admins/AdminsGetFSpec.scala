package io.paytouch.core.resources.admin.admins

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._

class AdminsGetFSpec extends AdminsFSpec {

  abstract class AdminsGetFSpecContext extends AdminResourceFSpecContext

  "GET /v1/admin/admins.get?admin_id=$" in {
    "if request has valid token" in {

      "return the admin information without a password" in new AdminsGetFSpecContext {

        Get(s"/v1/admin/admins.get?admin_id=${admin.id}").addHeader(adminAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val adminEntity = responseAs[ApiResponse[Admin]].data
          assertResponse(adminEntity, admin)
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new AdminsGetFSpecContext {

        Get(s"/v1/admin/admins.get?admin_id=${admin.id}")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
