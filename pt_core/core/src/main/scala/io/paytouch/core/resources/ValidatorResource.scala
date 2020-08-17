package io.paytouch.core.resources

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities.IdsToValidate
import io.paytouch.core.services.ValidatorService

trait ValidatorResource extends JsonResource {

  def validatorService: ValidatorService

  val validatorRoutes: Route =
    path("ids.validate") {
      post {
        entity(as[IdsToValidate]) { ids =>
          authenticate { implicit user =>
            onSuccess(validatorService.validate(ids))(result => completeAsEmptyResponse(result))
          }
        }
      }
    }
}
