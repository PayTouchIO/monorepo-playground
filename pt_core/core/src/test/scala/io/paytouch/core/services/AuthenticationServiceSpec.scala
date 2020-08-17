package io.paytouch.core.services

import com.softwaremill.macwire._

import io.paytouch.core.async.monitors.{ AuthenticationMonitor, SuccessfulLogin }
import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ContextSource
import io.paytouch.core.utils.{ AppTokenFixtures, DefaultFixtures, MockTime, PaytouchLogger, UtcTime }
import io.paytouch.utils.Tagging._

class AuthenticationServiceSpec extends ServiceDaoSpec {
  abstract class AuthenticationServiceSpecContext
      extends ServiceDaoSpecBaseContext
         with Fixtures
         with AppTokenFixtures {
    val monitor = actorMock.ref.taggedWith[AuthenticationMonitor]
    implicit val logger = new PaytouchLogger
    val authenticationService = wire[AuthenticationService]
  }

  "AuthenticationService" should {
    "login" should {
      "provide a jwt token if credentials are valid" in new AuthenticationServiceSpecContext {
        val result = authenticationService.login(credentials).await.success.get

        val responseJwtToken = result.valueToJwt
        responseJwtToken.userId ==== Some(user.id)
        responseJwtToken.source ==== Some(source)

        actorMock.expectMsgPF() {
          case SuccessfulLogin(_, loginSource, _) if loginSource === credentials.source => true
        }
      }

      "provide no token if credentials are not valid" in new AuthenticationServiceSpecContext {
        val result = authenticationService.login(credentials.copy(email = "foo")).await.success

        result ==== None

        actorMock.expectNoMessage()
      }
    }

    "getUserContext" should {
      "return a user for a valid token" in new AuthenticationServiceSpecContext {
        val result = authenticationService.getUserContext(jwtToken).await
        result ==== Some(userContext)
      }

      "not return a user if a token is not valid" in new AuthenticationServiceSpecContext {
        val invalidToken = JsonWebToken("not-a-valid-token")

        val result = authenticationService.getUserContext(invalidToken).await

        result ==== None
      }
    }

    "getOwnerUserContextForApp" should {
      "return a user for a valid app token (iss = pt_ordering)" in new AuthenticationServiceSpecContext {
        val token = appToken("pt_ordering")

        val result = authenticationService.getOwnerUserContextForApp(token).await
        result must beSome

        result.get.merchantId ==== merchant.id
        result.get.source ==== ContextSource.PtOrdering
      }

      "return a user for a valid app token in camel case format (iss = ptOrdering)" in new AuthenticationServiceSpecContext {
        val token = appToken("ptOrdering")

        val result = authenticationService.getOwnerUserContextForApp(token).await
        result must beSome

        result.get.merchantId ==== merchant.id
        result.get.source ==== ContextSource.PtOrdering
      }

      "return a user for a valid app token (iss = pt_delivery)" in new AuthenticationServiceSpecContext {
        val token = appToken("pt_delivery")

        val result = authenticationService.getOwnerUserContextForApp(token).await
        result must beSome

        result.get.merchantId ==== merchant.id
        result.get.source ==== ContextSource.PtDelivery
      }

      "not return a user for a unknown issuer" in new AuthenticationServiceSpecContext {
        val token = appToken("another-app")

        val result = authenticationService.getOwnerUserContextForApp(token).await
        result ==== None
      }

      "not return a user if a token is not valid" in new AuthenticationServiceSpecContext {
        val invalidToken = JsonWebToken("not-a-valid-token")

        val result = authenticationService.getOwnerUserContextForApp(invalidToken).await
        result ==== None
      }
    }

    "deleteByMerchantId" should {
      "delete all sessions owned by users of given merchant id" in new AuthenticationServiceSpecContext {
        jwtToken // trigger lazy evaluation and session creation
        authenticationService.deleteSessionsByMerchantId(merchant.id).await
        val maybeUserContext = authenticationService.getUserContext(jwtToken).await
        maybeUserContext must beNone
      }
    }
  }

  trait Fixtures extends DefaultFixtures {
    val credentials = LoginCredentials(email, password, source)
    val userLogin = user.toUserLogin
  }
}
