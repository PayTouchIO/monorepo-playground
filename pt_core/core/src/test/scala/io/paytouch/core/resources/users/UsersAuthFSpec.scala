package io.paytouch.core.resources.users

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.LoginSource
import io.paytouch.core.utils.{ DisabledUserFixtures, UtcTime, FixtureDaoFactory => Factory }

class UsersAuthFSpec extends UsersFSpec {

  abstract class UserAuthFSpecContext extends UserResourceFSpecContext

  "POST /v1/users.auth" in {

    "authenticate a user if credentials are valid" in new UserAuthFSpecContext {
      val credentials = LoginCredentials(user.email, password, source)
      Post("/v1/users.auth", credentials) ~> routes ~> check {
        assertStatusOK()

        val jwtTokenEntity = responseAs[ApiResponse[JsonWebToken]].data
        jwtTokenEntity.value.nonEmpty should beTrue
      }
    }

    "authenticate an owner user even if no user role" in new UserAuthFSpecContext {
      val ownerUser = Factory.user(merchant, password = Some(password), isOwner = Some(true)).create
      val credentials = LoginCredentials(ownerUser.email, password, source)
      Post("/v1/users.auth", credentials) ~> routes ~> check {
        assertStatusOK()

        val jwtTokenEntity = responseAs[ApiResponse[JsonWebToken]].data
        jwtTokenEntity.value.nonEmpty should beTrue
      }
    }

    "authenticate a user if credentials are valid - case insensitively" in new UserAuthFSpecContext {
      val userWithCapitalizedEmail = Factory
        .user(merchant, password = Some(password), isOwner = Some(true), email = Some(randomEmail.toUpperCase))
        .create
      val credentials = LoginCredentials(userWithCapitalizedEmail.email, password, source)
      Post("/v1/users.auth", credentials) ~> routes ~> check {
        assertStatusOK()

        val jwtTokenEntity = responseAs[ApiResponse[JsonWebToken]].data
        jwtTokenEntity.value.nonEmpty should beTrue
      }
    }

    "reject user authentication if user is deleted" in new UserAuthFSpecContext {
      val deletedUser = Factory.user(merchant, password = Some(password), deletedAt = Some(UtcTime.now)).create
      val credentials = LoginCredentials(deletedUser.email, password, source)
      Post("/v1/users.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "reject user authentication if user is disabled" in new UserAuthFSpecContext with DisabledUserFixtures {
      val credentials = LoginCredentials(disabledUser.email, password, source)
      Post("/v1/users.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "reject user authentication if user has no user role" in new UserAuthFSpecContext with DisabledUserFixtures {
      val noUserRoleUser = Factory.user(merchant, password = Some(password)).create
      val credentials = LoginCredentials(noUserRoleUser.email, password, source)
      Post("/v1/users.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "reject user authentication if user's user role has no access to source dashboard" in new UserAuthFSpecContext
      with DisabledUserFixtures {
      val userRoleNoDashboard = Factory.userRole(merchant, hasDashboardAccess = Some(false)).create
      val myUser = Factory.user(merchant, password = Some(password), userRole = Some(userRoleNoDashboard)).create
      val credentials = LoginCredentials(myUser.email, password, LoginSource.PtDashboard)
      Post("/v1/users.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "reject user authentication if user's user role has no access to source register" in new UserAuthFSpecContext
      with DisabledUserFixtures {
      val userRoleNoRegister = Factory.userRole(merchant, hasRegisterAccess = Some(false)).create
      val myUser = Factory.user(merchant, password = Some(password), userRole = Some(userRoleNoRegister)).create
      val credentials = LoginCredentials(myUser.email, password, LoginSource.PtRegister)
      Post("/v1/users.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "reject user authentication if user's user role has no access to source tickets" in new UserAuthFSpecContext
      with DisabledUserFixtures {
      val userRoleNoRegister = Factory.userRole(merchant, hasTicketsAccess = Some(false)).create
      val myUser = Factory.user(merchant, password = Some(password), userRole = Some(userRoleNoRegister)).create
      val credentials = LoginCredentials(myUser.email, password, LoginSource.PtTickets)
      Post("/v1/users.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Unauthorized)
      }
    }

    "reject user authentication if credentials are not valid" in new UserAuthFSpecContext {
      val credentials = LoginCredentials(user.email, "a-wrong-password", source)
      Post("/v1/users.auth", credentials) ~> routes ~> check {
        assertStatus(StatusCodes.Forbidden)
      }
    }
  }
}
