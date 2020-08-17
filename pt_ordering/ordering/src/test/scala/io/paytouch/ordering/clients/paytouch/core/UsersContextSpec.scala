package io.paytouch.ordering.clients.paytouch.core

import akka.http.scaladsl.model._
import io.paytouch.ordering.clients.paytouch.core.entities.CoreUserContext
import io.paytouch.ordering.errors.ClientError

class UsersContextSpec extends PtCoreClientSpec {

  abstract class UsersContextSpecContext extends CoreClientSpecContext {

    def assertUsersContext(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(HttpMethods.GET, "/v1/users.context", authToken)

  }

  "CoreClient" should {

    "call users.context" should {

      "parse user context" in new UsersContextSpecContext {
        val expectedUserContext = CoreUserContext(
          id = "13eede8d-c07a-307e-bfd6-6be95d897eaf",
          merchantId = "265cb372-4f28-3035-af49-e2f23e537c4e",
          locationIds = Seq(
            "b82b7b62-8251-3fd9-9e18-d73ab1adb918",
            "c6342289-5123-3971-908d-57a29b01bd24",
            "a07c7e10-713e-3050-8907-072c36eec741",
            "478a122b-37fc-318c-a70f-f95cdd699b78",
            "ac65f053-e966-3bdd-82f2-c8be7c02ad8c",
            "29f42b30-e479-3e2e-bd24-4003332f071d",
          ),
          currency = USD,
        )
        val response = when(usersContext)
          .expectRequest(implicit request => assertUsersContext)
          .respondWith("/core/responses/users_context.json")
        response.await.map(_.data) ==== Right(expectedUserContext)
      }

      "parse rejection" in new UsersContextSpecContext {
        val endpoint = completeUri("/v1/users.context")
        val expectedRejection = ClientError(endpoint, "The supplied authentication is invalid")
        val response = when(usersContext)
          .expectRequest(implicit request => assertUsersContext)
          .respondWithRejection("/core/responses/auth_rejection.json")
        response.await ==== Left(expectedRejection)
      }
    }
  }

}
