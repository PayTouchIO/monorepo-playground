package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.{ Directives, Route }
import io.paytouch.core.services.MerchantService

trait PingResource extends Directives {

  def merchantService: MerchantService

  val pingRoutes: Route = path("ping") {
    get {
      extractDataBytes { _ =>
        val reachTheDb = merchantService.findById(UUID.randomUUID)(merchantService.defaultExpansions)
        onSuccess(reachTheDb)(_ => complete("pong"))
      }
    }
  }
}
