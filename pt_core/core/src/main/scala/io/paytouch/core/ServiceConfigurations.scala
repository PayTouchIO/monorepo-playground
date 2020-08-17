package io.paytouch.core

import scala.collection.immutable.ArraySeq.unsafeWrapArray
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import akka.http.scaladsl.model.Uri

import com.typesafe.config.ConfigFactory

import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.clients.urbanairship.ProjectIds
import io.paytouch.core.clients.auth0.Auth0Config
import io.paytouch.core.data._
import io.paytouch.core.utils.RichString._
import io.paytouch.logging._
import io.paytouch.utils._
import io.paytouch.utils.Tagging._
import io.paytouch.implicits._
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm

trait BcryptRounds
trait CloudfrontImagesDistribution
trait CloudinaryUrl
trait HmacSecret
trait JwtSecret
trait JwtOrderingSecret
trait PtOrderingUser
trait PtOrderingPassword
trait PtCoreUrl
trait PusherKey
trait PusherSecret
trait S3CashDrawerActivitiesBucket
trait S3ImagesBucket
trait S3ExportsBucket
trait S3ImportsBucket
trait UrbanAirshipHost
trait UrbanAirshipPassword
trait UrbanAirshipUsername
trait UploadFolder
trait RedisHost
trait RedisPort
trait AdminPasswordAuthEnabled
trait GoogleAuthClientId
trait StripeBaseUri
trait StripeConnectUri
trait StripePublishableKey
trait StripeSecretKey

object ServiceConfigurations {
  protected val config = ConfigFactory.load()

  lazy val host: String = config.getString("http.host")
  lazy val port: Int = config.getInt("http.port")

  lazy val responseTimeout = 2.minutes.taggedWith[ResponseTimeout]

  lazy val pusherKey: String withTag PusherKey = config.getString("pusher.key").taggedWith[PusherKey]
  lazy val pusherSecret: String withTag PusherSecret = config.getString("pusher.secret").taggedWith[PusherSecret]

  lazy val JwtSecret: String withTag JwtSecret = config
    .getString("jwt.secret")
    .taggedWith[JwtSecret]

  lazy val JwtOrderingSecret: String withTag JwtOrderingSecret =
    config
      .getString("jwt.ordering_secret")
      .taggedWith[JwtOrderingSecret]

  lazy val bcryptRounds: Int withTag BcryptRounds = config.getInt("bcrypt.rounds").taggedWith[BcryptRounds]

  lazy val allowOrigins: List[String] withTag CorsAllowOrigins =
    config.getStringList("cors.allowOrigins").asScala.toList.taggedWith[CorsAllowOrigins]

  lazy val uploadFolder: String withTag UploadFolder = config.getString("uploads.directory").taggedWith[UploadFolder]

  // enforcing dash case as AWS doesn't allow camel case in buckets names
  lazy val s3CashDrawerActivitiesBucket: String = config.getString("uploads.s3.cash_drawer_activities").dashcase
  lazy val s3ImagesBucket: String = config.getString("uploads.images.s3").dashcase
  lazy val s3ExportsBucket: String = config.getString("uploads.s3.exports").dashcase
  lazy val s3ImportsBucket: String = config.getString("uploads.s3.imports").dashcase
  lazy val cloudfrontImagesDistribution: String withTag CloudfrontImagesDistribution =
    config.getString("uploads.images.cloudfrontUrl").taggedWith[CloudfrontImagesDistribution]
  lazy val cloudinaryUrl: String withTag CloudinaryUrl =
    config.getString("uploads.images.cloudinaryUrl").taggedWith[CloudinaryUrl]

  lazy val ptCoreURL: String withTag PtCoreUrl = config.getString("ptCoreURL").taggedWith[PtCoreUrl]

  lazy val ptCoreQueueNames: Seq[String] = unsafeWrapArray(config.getString("queues.ptCoreName").split(","))
  lazy val ptNotifierQueueNames: Seq[String] = unsafeWrapArray(config.getString("queues.ptNotifierName").split(","))
  lazy val ptDeliveryQueueNames: Seq[String] = unsafeWrapArray(config.getString("queues.ptDeliveryName").split(","))
  lazy val ptOrderingQueueNames: Seq[String] = unsafeWrapArray(config.getString("queues.ptOrderingName").split(","))
  lazy val sqsMsgCount: Int = config.getInt("queues.msgCount")

  lazy val hmacSecret: String withTag HmacSecret = config.getString("hmacSecret").taggedWith[HmacSecret]

  lazy val urbanAirshipHost: String withTag UrbanAirshipHost =
    config.getString("urbanairship.host").taggedWith[UrbanAirshipHost]
  lazy val urbanAirshipUsername: String withTag UrbanAirshipUsername =
    config.getString("urbanairship.username").taggedWith[UrbanAirshipUsername]
  lazy val urbanAirshipApiKey: String withTag UrbanAirshipPassword =
    config.getString("urbanairship.apiKey").taggedWith[UrbanAirshipPassword]
  lazy val urbanAirshipProjectIds: ProjectIds = ProjectIds(
    loyaltyProjectId = config.getString("urbanairship.loyaltyProjectId"),
    giftCardProjectId = config.getString("urbanairship.giftCardProjectId"),
  )

  object Regular {
    lazy val thumbnailImgSize: Int = config.getInt("image.size.regular.thumbnail")
    lazy val smallImgSize: Int = config.getInt("image.size.regular.small")
    lazy val mediumImgSize: Int = config.getInt("image.size.regular.medium")
    lazy val largeImgSize: Int = config.getInt("image.size.regular.large")
  }

  lazy val worldpay: model.PaymentProcessorConfig = {
    val subConfig = config.getConfig("worldpay.defaults")

    model
      .PaymentProcessorConfig
      .Worldpay(
        accountId = subConfig.getString("accountId"),
        acceptorId = subConfig.getString("acceptorId"),
        terminalId = subConfig.getString("terminalId"),
        accountToken = subConfig.getString("accountToken"),
      )
  }

  lazy val isTestEnv: Boolean = {
    val allowedPatterns = Seq("localhost", "127.0.0.1", "dev", "test")
    val dbUrl = config.getString("postgres.url")
    allowedPatterns.exists(p => dbUrl.toLowerCase.contains(p))
  }

  lazy val barcodeEmailHeight: Int = config.getInt("barcodes.email.height")
  lazy val barcodeEmailWidth: Int = config.getInt("barcodes.email.width")

  lazy val defaultMerchantWebsite: String = "http://www.paytouch.com/"

  lazy val demoImageUrl: String = config.getString("demoImages.baseUrl")

  lazy val logPostResponse: Boolean withTag LogPostResponse =
    config.getBoolean("logging.postResponse").taggedWith[LogPostResponse]
  lazy val logEndpointsToDebug: List[String] =
    config.getString("logging.endpointsToDebug").split(",").toList.filter(_.nonEmpty)

  lazy val ptOrderingUri: Uri withTag PtOrderingClient = Uri(config.getString("ordering.url"))
    .taggedWith[PtOrderingClient]
  lazy val ptOrderingUser: String withTag PtOrderingUser = config.getString("ordering.user").taggedWith[PtOrderingUser]
  lazy val ptOrderingPassword: String withTag PtOrderingPassword =
    config.getString("ordering.password").taggedWith[PtOrderingPassword]

  lazy val redisHost: String withTag RedisHost = config.getString("redis.host").taggedWith[RedisHost]
  lazy val redisPort: Int withTag RedisPort = config.getInt("redis.port").taggedWith[RedisPort]

  lazy val adminPasswordAuthEnabled: Boolean withTag AdminPasswordAuthEnabled =
    config.getBoolean("admin_password_auth.enabled").taggedWith[AdminPasswordAuthEnabled]

  lazy val googleAuthClientId: String withTag GoogleAuthClientId =
    config.getString("google_auth.client_id").taggedWith[GoogleAuthClientId]

  lazy val stripeBaseUri: Uri withTag StripeBaseUri =
    Uri(config.getString("stripe.base_uri")).taggedWith[StripeBaseUri]

  lazy val stripeConnectUri: Uri withTag StripeConnectUri =
    Uri(config.getString("stripe.connect_uri")).taggedWith[StripeConnectUri]

  lazy val stripePublishableKey: String withTag StripePublishableKey =
    config.getString("stripe.publishable_key").taggedWith[StripePublishableKey]

  lazy val stripeSecretKey: String withTag StripeSecretKey =
    config.getString("stripe.secret_key").taggedWith[StripeSecretKey]

  lazy val auth0Config: Auth0Config = {
    import Auth0Config._

    val subConfig = config.getConfig("auth0")

    val algorithm = JwtAlgorithm.fromString(subConfig.getString("algorithm")) match {
      case alg: JwtAsymmetricAlgorithm =>
        alg
      case _ =>
        throw new RuntimeException(
          "Auth0 algorithm must be of type JwtAssymetricAlgorithm. See https://github.com/pauldijou/jwt-scala/blob/88604833f4da474679db2617c7ffc67815b7dfe9/core/src/main/scala/JwtAlgorithm.scala#L67-L77",
        )
    }

    Auth0Config(
      algorithm = algorithm,
      apiIdentifier = subConfig.getString("api_identifier").pipe(ApiIdentifier),
      allowedIssuers = subConfig.getString("allowed_issuers").split(",").toSeq.map(_.pipe(AllowedIssuer)),
    )
  }

  lazy val AppHeaderName = "Paytouch-App-Name".taggedWith[AppHeaderName]
  lazy val VersionHeaderName = "Paytouch-App-Version".taggedWith[VersionHeaderName]
}
