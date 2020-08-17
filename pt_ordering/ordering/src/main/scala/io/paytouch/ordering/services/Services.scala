package io.paytouch.ordering.services

import scala.concurrent._

import akka.actor.ActorSystem
import akka.stream.Materializer

import com.softwaremill.macwire._

import io.paytouch.ordering.async.Actors
import io.paytouch.ordering.clients.google.GMapsClient
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.clients.stripe.StripeClient
import io.paytouch.ordering.clients.worldpay.WorldpayClient
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.data.redis.ConfiguredRedis
import io.paytouch.ordering.graphql.GraphQLService
import io.paytouch.ordering.messages.SQSMessageHandler
import io.paytouch.ordering.processors._
import io.paytouch.ordering.ServiceConfigurations

trait Services { self: Actors =>
  import ServiceConfigurations._

  implicit def ec: ExecutionContext
  implicit def db: Database

  implicit def materializer: Materializer

  implicit def asyncSystem: ActorSystem

  implicit def ptCoreClient: PtCoreClient
  implicit def gMapsClient: GMapsClient
  implicit def stripeClient: StripeClient
  implicit def worldpayClient: WorldpayClient
  implicit def redis: ConfiguredRedis

  implicit lazy val daos = new Daos

  lazy val authenticationService = wire[AuthenticationService]
  lazy val cartService: CartService = wire[CartService]
  lazy val cartSyncService: CartSyncService = wire[CartSyncService]
  lazy val cartItemService: CartItemService = wire[CartItemService]
  lazy val cartItemModifierOptionService: CartItemModifierOptionService =
    wire[CartItemModifierOptionService]
  lazy val cartItemTaxRateService: CartItemTaxRateService =
    wire[CartItemTaxRateService]
  lazy val cartItemVariantOptionService: CartItemVariantOptionService =
    wire[CartItemVariantOptionService]
  lazy val cartTaxRateService: CartTaxRateService = wire[CartTaxRateService]
  lazy val cartCheckoutService: CartCheckoutService = wire[CartCheckoutService]
  lazy val ekashuService: EkashuService = wire[EkashuService]
  lazy val gMapsService: GMapsService = wire[GMapsService]
  lazy val idService: IdService = wire[IdService]
  lazy val imageService: ImageService = wire[ImageService]
  lazy val jetDirectService: JetdirectService = wire[JetdirectService]
  lazy val merchantService: MerchantService = wire[MerchantService]
  lazy val paymentIntentService: PaymentIntentService =
    wire[PaymentIntentService]
  lazy val stripeService: StripeService = wire[StripeService]
  lazy val worldpayService: WorldpayService = wire[WorldpayService]

  /** graphQL services */
  lazy val catalogService: CatalogService = wire[CatalogService]
  lazy val categoryService: CategoryService = wire[CategoryService]
  lazy val coreMerchantService: CoreMerchantService = wire[CoreMerchantService]
  lazy val giftCardService: GiftCardService = wire[GiftCardService]
  lazy val graphQLService: GraphQLService = wire[GraphQLService]
  lazy val locationService: LocationService = wire[LocationService]
  lazy val orderService: OrderService = wire[OrderService]
  lazy val productService: ProductService = wire[ProductService]
  lazy val modifierSetService: ModifierSetService = wire[ModifierSetService]
  lazy val storeService: StoreService = wire[StoreService]
  lazy val tableService: TableService = wire[TableService]

  lazy val messageHandler = wire[SQSMessageHandler]

  lazy val merchantChangedProcessor = wire[MerchantChangedProcessor]
}
