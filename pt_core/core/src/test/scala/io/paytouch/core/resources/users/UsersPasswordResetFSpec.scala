package io.paytouch.core.resources.users

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class UsersPasswordResetFSpec extends UsersFSpec {
  abstract class UsersPasswordResetFSpecContext extends UserResourceFSpecContext {
    lazy val token = Factory.passwordResetToken(user).create

    lazy val body = PasswordReset(
      userId = user.id,
      token = token.key,
      password = "Pa$$word1234",
    )
  }

  "POST /v1/users.password_reset" in {
    "if the token is valid" should {
      "return 200" in new UsersPasswordResetFSpecContext {
        Post(s"/v1/users.password_reset", body) ~> routes ~> check {
          assertStatusOK()
        }
      }

      "return 401 for inactive users who are not owners" in new UsersPasswordResetFSpecContext {
        override lazy val user =
          Factory
            .user(
              merchant,
              firstName = Some(firstName),
              lastName = Some(lastName),
              password = Some(password),
              email = Some(email),
              locations = locations,
              userRole = Some(userRole),
              isOwner = Some(false), // employee
              pin = Some(userPin),
              active = Some(false), // inactive
            )
            .create

        Post(s"/v1/users.password_reset", body) ~> routes ~> check {
          assertStatus(StatusCodes.Unauthorized)
          assertErrorCode("UnauthorizedError")
        }
      }
    }

    "if the token is invalid" should {
      "return 404" in new UsersPasswordResetFSpecContext {
        val invalidBody = body.copy(token = "invalid")

        Post(s"/v1/users.password_reset", invalidBody) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if the token doesn't exist for the user" should {
      "return 404" in new UsersPasswordResetFSpecContext {
        val anotherUser = Factory
          .user(
            merchant,
          )
          .create
        val invalidBody = body.copy(userId = anotherUser.id)

        Post(s"/v1/users.password_reset", invalidBody) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if the token is expired" should {
      "return 404" in new UsersPasswordResetFSpecContext {
        override lazy val token = Factory.passwordResetToken(user, expiresAt = Some(UtcTime.now.minusMinutes(1))).create
        Post(s"/v1/users.password_reset", body) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if the token has already been used" should {
      "return 404" in new UsersPasswordResetFSpecContext {
        Post(s"/v1/users.password_reset", body) ~> routes ~> check {
          assertStatusOK()
        }

        Post(s"/v1/users.password_reset", body) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if the password doesn't meet requirements" should {
      "reject the request" in new UsersPasswordResetFSpecContext {
        val invalidBody = body.copy(password = "1234")

        Post(s"/v1/users.password_reset", invalidBody) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
          assertErrorCodesAtLeastOnce("InvalidPassword")
        }
      }
    }
  }
}
