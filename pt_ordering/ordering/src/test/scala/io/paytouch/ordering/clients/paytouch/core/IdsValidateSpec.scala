package io.paytouch.ordering.clients.paytouch.core

import java.util.UUID

import akka.http.scaladsl.model._
import io.paytouch.ordering.clients.paytouch.core.entities.CoreIds

class IdsValidateSpec extends PtCoreClientSpec {

  abstract class IdsValidateSpecContext extends CoreClientSpecContext {
    val coreIds = CoreIds(locationIds = Seq(UUID.randomUUID), catalogIds = Seq(UUID.randomUUID))

    def assertIdsValidate(ids: CoreIds)(implicit request: HttpRequest, authToken: HttpHeader) =
      assertUserAuthRequest(HttpMethods.POST, "/v1/ids.validate", authToken, body = Some(ids))
  }

  "CoreClient" should {

    "call ids.validate" should {

      "parse empty response" in new IdsValidateSpecContext {
        val response =
          when(idsValidate(coreIds)).expectRequest(implicit request => assertIdsValidate(coreIds)).respondWithNoContent
        response.await ==== Right(())
      }

      "parse rejection" in new IdsValidateSpecContext {
        val response = when(idsValidate(coreIds))
          .expectRequest(implicit request => assertIdsValidate(coreIds))
          .respondWithRejection("/core/responses/ids_validate_rejection.json")
        response.await.isLeft should beTrue
      }
    }
  }

}
