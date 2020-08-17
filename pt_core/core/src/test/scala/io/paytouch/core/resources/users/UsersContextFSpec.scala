package io.paytouch.core.resources.users

import akka.http.scaladsl.server.AuthenticationFailedRejection
import io.paytouch.core.entities._

class UsersContextFSpec extends UsersFSpec {

  abstract class UsersContextFSpecContext extends UserResourceFSpecContext

  "GET /v1/users.context" in {
    "if request has valid token" in {

      "return the user context associated to the token" in new UsersContextFSpecContext {
        Get(s"/v1/users.context")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatusOK()

          val entity = responseAs[ApiResponse[UserContext]].data
          val sortedEntity = entity.copy(locationIds = entity.locationIds.sorted)
          val sortedExpectation =
            userContext.copy(merchantSetupCompleted = false, locationIds = userContext.locationIds.sorted)
          sortedEntity ==== sortedExpectation
        }
      }
    }

    "if request has invalid token" should {

      "be rejected" in new UsersContextFSpecContext {

        Get(s"/v1/users.context").addHeader(invalidAuthorizationHeader) ~> routes ~> check {
          rejection should beAnInstanceOf[AuthenticationFailedRejection]
        }
      }
    }
  }
}
