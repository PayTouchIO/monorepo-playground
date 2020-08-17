package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.services.OauthService

trait OauthResource extends JsonResource {

  def oauthService: OauthService

  val oauthRoutes: Route =
    path("oauth.authorize") {
      post {
        parameters(
          "client_id".as[UUID],
          "redirect_uri",
        ) {
          case (clientId, redirectUri) =>
            authenticate { implicit user =>
              onSuccess(oauthService.authorize(clientId, redirectUri))(result => completeAsApiResponse(result))
            }
        }
      }
    } ~
      path("oauth.verify") {
        post {
          val verify: (UUID, UUID, UUID) => Route = {
            case (clientId, clientSecret, code) =>
              onSuccess(oauthService.verify(clientId, clientSecret, code)) { result =>
                completeAsValidatedApiResponse(result)
              }
          }
          concat(
            requestEntityEmpty {
              parameters(
                "client_id".as[UUID],
                "client_secret".as[UUID],
                "code".as[UUID],
              )(verify)
            },
            requestEntityPresent {
              formFields(
                "client_id".as[UUID],
                "client_secret".as[UUID],
                "code".as[UUID],
              )(verify)
            },
          )
        }
      }
}
