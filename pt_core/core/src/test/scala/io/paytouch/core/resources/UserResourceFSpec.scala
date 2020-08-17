package io.paytouch.core.resources

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.entities.enums.LoginSource
import io.paytouch.core.entities.LoginCredentials
import io.paytouch.core.utils.FSpec

class UserResourceFSpec extends FSpec {
  "A users.auth request" should {
    "when the credentials are wrong" should {
      "reply with forbidden" in new FSpecContext {
        val credentials = LoginCredentials("email@email.com", "wrong-password", LoginSource.PtDashboard)

        Post("/v1/users.auth", credentials) ~> routes ~> check {
          assertStatus(StatusCodes.Forbidden)
        }
      }
    }
  }
}
