package io.paytouch.core.resources.users

import akka.http.scaladsl.model.StatusCodes

import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

class UsersStartPasswordResetFSpec extends UsersFSpec {
  "POST /v1/users.start_password_reset" in {
    "if a user with the email exists" should {
      "return 204" in new UserResourceFSpecContext {
        Post(s"/v1/users.start_password_reset?email=${user.email}") ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
        }
      }

      "return 204 even if the user is inactive and not owner" in new UserResourceFSpecContext {
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

        Post(s"/v1/users.start_password_reset?email=${user.email}") ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
        }
      }
    }

    "if a user with the email does not exist" should {
      "return 204" in new UserResourceFSpecContext {
        Post(s"/v1/users.start_password_reset?email=nobody@nobody.com") ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
        }
      }
    }

    "if the email is invalid" should {
      "return 204" in new UserResourceFSpecContext {
        Post("/v1/users.start_password_reset?email=nobody") ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
        }
      }
    }
  }
}
