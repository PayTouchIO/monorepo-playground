package io.paytouch.ordering.resources.features

import akka.http.scaladsl.server.Route
import io.paytouch.ordering.entities.UserContext

trait StandardUserResource extends StandardResource {

  type Context = UserContext

  protected def contextAuthentication(f: Context => Route): Route = userAuthenticate(f)
}
