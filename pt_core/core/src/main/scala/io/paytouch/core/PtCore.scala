package io.paytouch.core

import akka.actor._
import akka.util.Timeout

import com.softwaremill.macwire._

import io.paytouch.core.{ ServiceConfigurations => Config }
import io.paytouch.core.clients.auth0._
import io.paytouch.core.clients.aws.S3Client
import io.paytouch.core.clients.paytouch.ordering.PtOrderingClient
import io.paytouch.core.clients.stripe._
import io.paytouch.core.data.db.{ ConfiguredDatabase, SlowOpsDatabase }
import io.paytouch.core.data.redis.ConfiguredRedis
import io.paytouch.core.logging.MdcActor
import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.Tagging._

trait PtCore extends App with RestApi {
  implicit lazy val system = ActorSystem("pt_core")

  implicit lazy val ec = system.dispatcher
  implicit lazy val timeout = Timeout(Config.responseTimeout)

  implicit lazy val db = ConfiguredDatabase.db

  lazy val slowOpsDb = SlowOpsDatabase.db

  implicit lazy val mdcActor: ActorRef withTag BaseMdcActor =
    system.actorOf(Props[MdcActor](), "pt_core_mdc_actor").taggedWith[BaseMdcActor]

  lazy val s3Client = wire[S3Client]

  lazy val ptOrderingUri = Config.ptOrderingUri
  lazy val prOrderingUser = Config.ptOrderingUser
  lazy val prOrderingPassword = Config.ptOrderingPassword
  lazy val ptOrderingClient = wire[PtOrderingClient]

  lazy val redisHost = Config.redisHost
  lazy val redisPort = Config.redisPort
  lazy val redis = wire[ConfiguredRedis]

  lazy val stripeBaseUri = Config.stripeBaseUri
  lazy val stripeConnectUri = Config.stripeConnectUri
  lazy val stripePublishableKey = Config.stripePublishableKey
  lazy val stripeSecretKey = Config.stripeSecretKey
  lazy val stripeClient = wire[StripeClient]
  lazy val stripeConnectClient = wire[StripeConnectClient]

  lazy val auth0Config = Config.auth0Config
  lazy val jwkClient = wire[JwkClient]
  lazy val auth0Client = wire[Auth0Client]

  lazy val AppHeaderName = ServiceConfigurations.AppHeaderName
  lazy val VersionHeaderName = ServiceConfigurations.VersionHeaderName
  lazy val corsAllowOrigins = ServiceConfigurations.allowOrigins

  lazy val api = routes
}
