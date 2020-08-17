package io.paytouch.ordering

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import akka.http.scaladsl.model.Uri

import com.typesafe.config.ConfigFactory

import io.paytouch._
import io.paytouch.implicits._

import io.paytouch.logging._
import io.paytouch.ordering.clients.google.GMapsClient
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.clients.stripe.StripeClientConfig
import io.paytouch.ordering.clients.worldpay._
import io.paytouch.ordering.entities.stripe.Livemode
import io.paytouch.ordering.data.redis._
import io.paytouch.utils._

// Do NOT use LAZY vals or defs in here.
// We want to make sure that no values are missing from the config.
object ServiceConfigurations {
  private val config = ConfigFactory.load()

  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val responseTimeout = 2.minutes.taggedWith[ResponseTimeout]

  val allowOrigins = config
    .getStringList("cors.allowOrigins")
    .asScala
    .toList
    .taggedWith[CorsAllowOrigins]

  val logPostResponse =
    config.getBoolean("logging.postResponse").taggedWith[LogPostResponse]
  val logEndpointsToDebug = config
    .getString("logging.endpointsToDebug")
    .split(",")
    .toList
    .filter(_.nonEmpty)

  val storeUser = config.getString("storefront.user")
  val storePassword = config.getString("storefront.password")

  val coreUser = config.getString("core.user")
  val corePassword = config.getString("core.password")
  val coreUri = Uri(config.getString("core.uri")).taggedWith[PtCoreClient]
  val coreJwtOrderingSecret = config.getString("core.jwt_ordering_secret")

  val orderingUri = Uri(config.getString("ordering.uri"))

  val sqsMsgCount: Int = config.getInt("queues.msgCount")
  val ptOrderingQueueName: String = config.getString("queues.ptOrderingName")
  val ptCoreQueueName: String = config.getString("queues.ptCoreName")

  val googleKey = config.getString("google.key").taggedWith[GMapsClient]

  val worldpayTransactionEndpointUri = Uri(config.getString("worldpay.transaction_endpoint_uri"))
    .taggedWith[WorldpayTransactionEndpointUri]
  val worldpayReportingEndpointUri = Uri(config.getString("worldpay.reporting_endpoint_uri"))
    .taggedWith[WorldpayReportingEndpointUri]
  val worldpayCheckoutUri = Uri(config.getString("worldpay.checkout_uri"))
    .taggedWith[WorldpayCheckoutUri]
  val worldpayApplicationId = config
    .getString("worldpay.application_id")
    .taggedWith[WorldpayApplicationId]
  val worldpayReturnUri = Uri(config.getString("worldpay.return_uri"))
    .taggedWith[WorldpayReturnUri]

  val stripeClientConfig: StripeClientConfig = {
    import StripeClientConfig._

    val subConfig = config.getConfig("stripe")

    StripeClientConfig(
      applicationFeeBasePoints = subConfig.getInt("application_fee_base_points").pipe(ApplicationFeeBasePoints),
      baseUri = subConfig.getString("base_uri").pipe(Uri.apply).pipe(BaseUri),
      secretKey = subConfig.getString("secret_key").pipe(SecretKey),
      webhookSecret = subConfig.getString("webhook_secret").pipe(WebhookSecret),
      livemode = subConfig.getBoolean("livemode").pipe(Livemode),
    )
  }

  val featureConfig: FeatureConfig = {
    import FeatureConfig._

    val subConfig = config.getConfig("feature")

    FeatureConfig(
      subConfig.getBoolean("useStorePaymentTransaction").pipe(UseStorePaymentTransaction),
    )
  }

  val redisHost: String withTag RedisHost = config.getString("redis.host").taggedWith[RedisHost]
  val redisPort: Int withTag RedisPort = config.getInt("redis.port").taggedWith[RedisPort]

  val isDevelopment = config.getBoolean("is_development")

  val AppHeaderName = "Paytouch-App-Name".taggedWith[AppHeaderName]
  val VersionHeaderName = "Paytouch-App-Version".taggedWith[VersionHeaderName]
}
