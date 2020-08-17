package io.paytouch.core.resources

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route

import cats.data.Validated

import io.paytouch.core.entities._
import io.paytouch.core.errors.Errors
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.services.PusherService
import io.paytouch.json.JsonMarshaller

trait PusherResource extends FormDataResource with JsonMarshaller with JsonSupport {
  def pusherService: PusherService

  val pusherRoutes: Route =
    path("pusher.auth") {
      post {
        formFields("channel_name", "socket_id").as(PusherAuthentication) {
          case (pusherAuthentication) =>
            authenticate { implicit user =>
              pusherService.authenticate(pusherAuthentication) match {
                case Validated.Valid(pusherAuth) => complete(pusherAuth)
                case Validated.Invalid(i)        => complete(Forbidden, Errors(i))
              }
            }
        }
      }
    }
}
