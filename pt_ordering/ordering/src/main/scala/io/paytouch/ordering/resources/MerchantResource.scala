package io.paytouch.ordering.resources

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import io.paytouch.ordering.UpsertionResult
import io.paytouch.ordering.entities.{ ExposedEntity, UserContext }
import io.paytouch.ordering.resources.features.JsonResource
import io.paytouch.ordering.services.{ AuthenticationService, MerchantService }
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

import scala.concurrent.Future

class MerchantResource(val authenticationService: AuthenticationService, val merchantService: MerchantService)
    extends JsonResource {

  val resourcePath = "merchants"

  lazy val routes: Route =
    meRoute(implicit user => merchantService.find) ~
      updateRoute(implicit user => merchantService.update) ~
      validateUrlSlugRoute(implicit user => merchantService.validateUrlSlug)

  private def meRoute[T <: ExposedEntity](f: UserContext => Future[Option[T]]) =
    (path(s"$resourcePath.me") & get) {
      userAuthenticate(user => onSuccess(f(user))(result => completeAsOptApiResponse(result)))
    }

  private def updateRoute[U: FromRequestUnmarshaller, T <: ExposedEntity](
      f: UserContext => U => Future[UpsertionResult[T]],
    ) =
    insertRoute("update", f)

  private def insertRoute[I: FromRequestUnmarshaller, T <: ExposedEntity](
      verbPath: String,
      f: UserContext => I => Future[UpsertionResult[T]],
    ) =
    (path(s"$resourcePath.$verbPath") & post) {
      entity(as[I]) { insert =>
        userAuthenticate(user => onSuccess(f(user)(insert))(result => completeAsApiResponse(result)))
      }
    }

  private def validateUrlSlugRoute(f: UserContext => String => Future[ValidatedData[Unit]]) =
    (path(s"$resourcePath.validate_url_slug") & get) {
      parameters(s"url_slug") { urlSlug =>
        userAuthenticate(user => onSuccess(f(user)(urlSlug))(result => completeAsEmptyResponse(result)))
      }
    }
}
