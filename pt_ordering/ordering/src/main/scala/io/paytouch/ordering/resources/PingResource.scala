package io.paytouch.ordering.resources

import akka.http.scaladsl.server.{ Directives, Route }

class PingResource extends Directives {

  val routes: Route = path("ping") {
    get {
      extractDataBytes(_ => complete("pong"))
    }
  }
}
