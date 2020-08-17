package io.paytouch.ordering.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.ordering.entities.{ Ids, IdsUsage }
import io.paytouch.ordering.resources.features.JsonResource
import io.paytouch.ordering.services.{ AuthenticationService, IdService }

import scala.concurrent.Future

class IdResource(val authenticationService: AuthenticationService, val idService: IdService) extends JsonResource {

  val resourcePath = "ids"

  lazy val routes: Route = checkUsageRoute(idService.checkUsage)

  private def checkUsageRoute(f: (UUID, Ids) => Future[IdsUsage]) =
    (path(s"$resourcePath.check_usage") & post) {
      parameters("merchant_id".as[UUID]) { merchantId =>
        entity(as[Ids]) { idsToCheck =>
          appAuthenticate(_ => onSuccess(f(merchantId, idsToCheck))(result => completeAsApiResponse(result)))
        }
      }
    }
}
