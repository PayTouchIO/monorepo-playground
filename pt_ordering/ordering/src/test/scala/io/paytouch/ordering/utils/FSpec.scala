package io.paytouch.ordering
package utils

import akka.http.scaladsl.model._

import org.specs2.matcher.Matcher
import org.specs2.specification.Scope

abstract class FSpec extends DaoSpec with PaytouchRouteSpec {
  abstract class FSpecContext extends Scope {
    val routes = MockedRestApi.routes

    lazy val ptCoreClient = MockedRestApi.ptCoreClient

    def assertRedirect(uri: Uri) = {
      assertStatus(StatusCodes.Found)

      val maybeHeaderValue = header(LocationHeaderName)
      maybeHeaderValue must beSome
      maybeHeaderValue.get.value() must contain(uri.toString)
    }

    def assertErrorCode(expectedErrorCode: String) = {
      val maybeHeaderValue = header(ValidationHeaderName)
      maybeHeaderValue must beSome
      maybeHeaderValue.get.value() must contain(expectedErrorCode)
    }

    def assertNoErrorCode() = {
      val maybeHeaderValue = header(ValidationHeaderName)
      maybeHeaderValue must beNone
    }

    def assertStatusCreated() = assertStatus(StatusCodes.Created)
    def assertStatusOK() = assertStatus(StatusCodes.OK)
    def assertStatus(expectedStatus: StatusCode) = status should beStatus(expectedStatus)

    private def beStatus(expectedStatus: StatusCode): Matcher[StatusCode] = { s: StatusCode =>
      (
        s == expectedStatus,
        s"$status == $expectedStatus",
        s"$status != $expectedStatus (response: $response)",
      )
    }
  }
}

class MockedRestApiShutdown {
  import akka.testkit.TestKit
  TestKit.shutdownActorSystem(MockedRestApi.testAsyncSystem)
}
