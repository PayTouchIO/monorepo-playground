package io.paytouch.ordering.resources

import com.softwaremill.macwire._

import io.paytouch.ordering.async.Actors
import io.paytouch.ordering.graphql.GraphQLResource
import io.paytouch.ordering.services.Services

trait Resources extends Services with Actors {
  lazy val cartResource = wire[CartResource]
  lazy val cartItemResource = wire[CartItemResource]
  lazy val ekashuResource = wire[EkashuResource]
  lazy val jetDirectResource = wire[JetdirectResource]
  lazy val graphQLResource = wire[GraphQLResource]
  lazy val idResource = wire[IdResource]
  lazy val merchantResource = wire[MerchantResource]
  lazy val paymentIntentResource = wire[PaymentIntentResource]
  lazy val pingResource = wire[PingResource]
  lazy val storeResource = wire[StoreResource]
  lazy val stripeResource = wire[StripeResource]
  lazy val swaggerResource = wire[SwaggerResource]
  lazy val worldpayResource = wire[WorldpayResource]
}
