package io.paytouch.ordering.resources

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.ordering.entities.enums.PaymentProcessorCallbackStatus
import io.paytouch.ordering.resources.features.JsonResource
import io.paytouch.ordering.services.{ AuthenticationService, EkashuService }

class EkashuResource(val authenticationService: AuthenticationService, val ekashuService: EkashuService)
    extends JsonResource
       with LazyLogging {

  lazy val routes: Route =
    path("vendor" / "ekashu" / "callbacks" / "success") {
      post {
        formFieldMap { fields =>
          val status = PaymentProcessorCallbackStatus.Success
          onSuccess(ekashuService.processCallback(fields, status)) { result =>
            if (result.isInvalid) {
              logger.error(s"While processing ekashu callback: $result. {status -> $status; fields -> $fields}")
              completeAsEmptyResponse(result)
            }
            else complete(StatusCodes.OK, None)
          }
        }
      }
    }
}
