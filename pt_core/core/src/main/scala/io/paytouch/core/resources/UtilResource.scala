package io.paytouch.core.resources

import scala.concurrent._

import cats.implicits._

import akka.http.scaladsl.server.Route

import io.paytouch._

import io.paytouch.core.services.UtilService

trait UtilResource extends JsonResource {
  val utilRoutes: Route =
    path("utils.countries") {
      get {
        authenticate { implicit user =>
          onSuccess(UtilService.Geo.countries.pure[Future])(result => completeSeqAsApiResponse(result))
        }
      }
    } ~
      path("utils.states") {
        parameter("country_code") { countryCode =>
          get {
            authenticate { implicit user =>
              onSuccess(UtilService.Geo.states(CountryCode(countryCode)).pure[Future]) { result =>
                completeSeqAsApiResponse(result)
              }
            }
          }
        }
      } ~
      path("utils.time_zones") {
        get {
          authenticate { implicit user =>
            onSuccess(UtilService.Geo.timezones.pure[Future])(result => completeSeqAsApiResponse(result))
          }
        }
      }
}
