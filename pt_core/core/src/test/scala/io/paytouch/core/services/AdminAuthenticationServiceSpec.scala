package io.paytouch.core.services

import java.util.UUID

import scala.concurrent._

import cats.implicits._

import com.github.t3hnar.bcrypt._

import io.paytouch.core.{ AdminPasswordAuthEnabled, JwtSecret }
import io.paytouch.core.async.monitors.{ AdminAuthenticationMonitor, SuccessfulAdminLogin }
import io.paytouch.core.entities._
import io.paytouch.core.errors.AdminPasswordAuthDisabledError
import io.paytouch.core.utils.{ MockTime, UtcTime, Multiple, FixtureDaoFactory => Factory }
import io.paytouch.utils.Tagging._

class AdminAuthenticationServiceSpec extends ServiceDaoSpec {
  abstract class AdminAuthenticationServiceSpecContext extends ServiceDaoSpecBaseContext with Fixtures {
    val monitor = actorMock.ref.taggedWith[AdminAuthenticationMonitor]
    val adminService = mock[AdminService]

    val adminAuthenticationService =
      new AdminAuthenticationService(
        jwtSecret.taggedWith[JwtSecret],
        adminPasswordAuthEnabled.taggedWith[AdminPasswordAuthEnabled],
        monitor,
        adminService,
        googleAuthenticationService,
      ) {
        override def thisInstant = MockTime.thisInstant
        override def generateUuid = uuid
      }
  }

  "AdminAuthenticationService" should {
    "login" should {
      "provide a jwt token if credentials are valid" in new AdminAuthenticationServiceSpecContext {
        adminService.findAdminInfoByEmail(anyString) returns adminLogin.some.pure[Future]

        val result = adminAuthenticationService.login(credentials).await.success

        result ==== Some(LoginResponse(token.value, None))
        there was one(adminService).findAdminInfoByEmail(email)

        actorMock.expectMsg(SuccessfulAdminLogin(adminLogin, UtcTime.ofInstant(MockTime.thisInstant)))
      }

      "provide no token if credentials are not valid" in new AdminAuthenticationServiceSpecContext {
        adminService.findAdminInfoByEmail(anyString) returns Future.successful(None)

        val result = adminAuthenticationService.login(credentials).await.success

        result ==== None
        there was one(adminService).findAdminInfoByEmail(email)
        actorMock.expectNoMessage()
      }

      "return an error if disabled" in new AdminAuthenticationServiceSpecContext {
        override val adminPasswordAuthEnabled = false

        adminService.findAdminInfoByEmail(anyString) returns adminLogin.some.pure[Future]

        val result = adminAuthenticationService.login(credentials).await
        result ==== Multiple.failure(AdminPasswordAuthDisabledError())

        there was no(adminService).findAdminInfoByEmail(email)
        actorMock.expectNoMessage()
      }
    }
  }

  trait Fixtures {
    val uuid = UUID.randomUUID
    val cleanUuid = uuid.toString.replace("-", "")
    val secret = "aSecret"

    val jwtSecret = "aJwtSecret"
    val adminPasswordAuthEnabled = true
    val bcryptRounds = 10

    val firstName = "aFirstName"
    val lastName = "aLastName"
    val email = "email@email"
    val password = "aPassword"

    val credentials = AdminLoginCredentials(email, password)

    val encryptedPassword = password.bcrypt(bcryptRounds)

    val merchant = Factory.merchant.create

    val adminLogin = AdminLogin(id = uuid, email = email, encryptedPassword = encryptedPassword)

    val token = JsonWebToken(
      Map(
        "uid" -> uuid.toString,
        "iss" -> "ptCore",
        "iat" -> 1451912083L,
        "jti" -> cleanUuid,
      ),
      jwtSecret,
    )
  }
}
