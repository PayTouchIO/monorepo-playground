package io.paytouch.ordering.resources

import akka.http.scaladsl.server.Route
import io.paytouch.ordering.resources.features.StandardUserResource
import io.paytouch.ordering.services.{ AuthenticationService, StoreService }

class StoreResource(val authenticationService: AuthenticationService, val storeService: StoreService)
    extends StandardUserResource {

  val resourcePath = "stores"
  val paramName = "store_id"

  lazy val routes: Route =
    Seq(
      createRoute(implicit user => storeService.create),
      getRoute(implicit user => storeService.findById),
      listRoute(implicit user => implicit pagination => storeService.findAll()),
      updateActiveRoute(implicit user => storeService.updateActive),
      updateRoute(implicit user => storeService.update),
    ).reduceLeft(_ ~ _)
}
