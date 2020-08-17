package io.paytouch.ordering

import akka.http.scaladsl.server._
import io.paytouch.ordering.async.Actors
import io.paytouch.ordering.async.sqs.SQSConsumer
import io.paytouch.ordering.logging.HttpLogging
import io.paytouch.ordering.resources.Resources
import io.paytouch.ordering.utils.CustomHandlers
import io.paytouch.utils.CorsSupport

trait RestApi extends Resources with HttpLogging with CorsSupport with CustomHandlers with Actors with SQSConsumer {
  override val responseTimeout = ServiceConfigurations.responseTimeout

  lazy val routes: Route =
    customLogRequestResponse {
      cors {
        pingRoutes ~
          graphQLRoutes ~
          pathPrefix("v1") {
            apiRoutes
          } ~
          rejectNonMatchedRoutes
      }
    }

  private lazy val pingRoutes = pingResource.routes
  private lazy val graphQLRoutes = graphQLResource.routes

  private lazy val apiRoutes =
    cartResource.routes ~
      cartItemResource.routes ~
      ekashuResource.routes ~
      jetDirectResource.routes ~
      idResource.routes ~
      merchantResource.routes ~
      paymentIntentResource.routes ~
      storeResource.routes ~
      stripeResource.routes ~
      swaggerResource.routes ~
      worldpayResource.routes
}
