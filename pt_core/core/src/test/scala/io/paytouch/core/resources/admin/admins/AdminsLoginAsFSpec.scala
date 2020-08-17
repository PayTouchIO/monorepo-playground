package io.paytouch.core.resources.admin.admins

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._
import io.paytouch.core.utils.DefaultFixtures

class AdminsLoginAsFSpec extends AdminsFSpec {

  abstract class AdminsLoginAsFSpecContext extends AdminResourceFSpecContext with DefaultFixtures

  "GET /v1/admin/admins.login_as?user_id=$&source=$" in {
    "if request has valid token" in {

      "return a token to login as a given user" in new AdminsLoginAsFSpecContext {

        Post(s"/v1/admin/admins.login_as?user_id=${user.id}&source=${source.entryName}")
          .addHeader(adminAuthorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val userJwtToken = responseAs[ApiResponse[JsonWebToken]].data
          userJwtToken.value.nonEmpty should beTrue

          val session = sessionDao.access(userJwtToken).await
          session should beSome
          session.get.userId ==== user.id
          session.get.adminId ==== Some(admin.id)

          val userAuthorizationHeader = Authorization(OAuth2BearerToken(userJwtToken))

          Get(s"/v1/users.me").addHeader(userAuthorizationHeader) ~> routes ~> check {
            assertStatusOK()
          }
        }
      }

      "return 404 if the user id does not exist" in new AdminsLoginAsFSpecContext {

        Post(s"/v1/admin/admins.login_as?user_id=${UUID.randomUUID}&source=${source.entryName}")
          .addHeader(adminAuthorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new AdminsLoginAsFSpecContext {

        Post(s"/v1/admin/admins.login_as?user_id=${user.id}&source=${source.entryName}")
          .addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
