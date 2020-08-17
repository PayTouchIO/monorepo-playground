package io.paytouch.core.resources.admin.admins

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._

class AdminsAuthFSpec extends AdminsFSpec {

  abstract class AdminAuthFSpecContext extends AdminResourceFSpecContext

  "POST /v1/admin/admins.auth" in {

    "authenticate a admin if credentials are valid" in new AdminAuthFSpecContext {
      val credentials = AdminLoginCredentials(admin.email, adminPassword)
      Post("/v1/admin/admins.auth", credentials) ~> routes ~> check {
        assertStatusOK()

        val jwtTokenEntity = responseAs[ApiResponse[JsonWebToken]].data
        jwtTokenEntity.value.nonEmpty should beTrue
      }
    }

    "reject admin authentication if credentials are not valid" in new AdminAuthFSpecContext {
      val credentials = AdminLoginCredentials(admin.email, "a-wrong-admin-password")
      Post("/v1/admin/admins.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Forbidden)
      }
    }
  }
}
