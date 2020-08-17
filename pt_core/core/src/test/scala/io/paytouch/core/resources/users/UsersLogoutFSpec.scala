package io.paytouch.core.resources.users

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.AuthenticationFailedRejection

class UsersLogoutFSpec extends UsersFSpec {

  abstract class UsersLogoutFSpecContext extends UserResourceFSpecContext

  "POST /v1/users.logout" in {
    "if request has valid token" in {

      "delete the session" in new UsersLogoutFSpecContext {

        Post("/v1/users.logout").addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
          sessionDao.findByUserId(user.id).await ==== Seq.empty
        }

        Get("/v1/users.me").addHeader(authorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new UsersLogoutFSpecContext {

        Post(s"/v1/users.logout").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
