package io.paytouch.ordering

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.Materializer
import akka.util.Timeout
import com.softwaremill.macwire._
import com.softwaremill.sttp.HttpURLConnectionBackend
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.logging.BaseMdcActor
import io.paytouch.ordering.clients.google.GMapsClient
import io.paytouch.ordering.clients.paytouch.core.PtCoreClient
import io.paytouch.ordering.clients.stripe.{ StripeClient, StripeClientConfig }
import io.paytouch.ordering.clients.worldpay.WorldpayClient
import io.paytouch.ordering.data.db.ConfiguredDatabase
import io.paytouch.ordering.data.redis._
import io.paytouch.ordering.logging.MdcActor
import io.paytouch.ordering.{ ServiceConfigurations => Config }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

object Main extends App with RestApi with LazyLogging {
  val forceInitializationForAllValues = Config.toString // everything inside is a val

  implicit lazy val system = ActorSystem("pt_ordering")
  implicit lazy val materializer = Materializer(system)

  implicit lazy val ec = system.dispatcher
  implicit lazy val timeout = Timeout(Config.responseTimeout)

  implicit lazy val db = ConfiguredDatabase.db

  lazy val asyncSystem = system

  implicit lazy val mdcActor: ActorRef withTag BaseMdcActor =
    system.actorOf(Props[MdcActor](), "pt_core_mdc_actor").taggedWith[BaseMdcActor]

  implicit lazy val sttpBackend = HttpURLConnectionBackend()

  lazy val AppHeaderName = ServiceConfigurations.AppHeaderName
  lazy val VersionHeaderName = ServiceConfigurations.VersionHeaderName
  lazy val corsAllowOrigins = ServiceConfigurations.allowOrigins

  lazy val ptCoreClient = {
    val uri = Config.coreUri
    wire[PtCoreClient]
  }

  lazy val gMapsClient = {
    val key = Config.googleKey
    wire[GMapsClient]
  }

  lazy val worldpayClient = {
    val transactionEndpointUri = Config.worldpayTransactionEndpointUri
    val reportingEndpointUri = Config.worldpayReportingEndpointUri
    val checkoutUri = Config.worldpayCheckoutUri
    val applicationId = Config.worldpayApplicationId
    val returnUri = Config.worldpayReturnUri
    wire[WorldpayClient]
  }

  lazy val stripeClient = {
    val config = Config.stripeClientConfig
    wire[StripeClient]
  }

  private lazy val redisHost = Config.redisHost
  private lazy val redisPort = Config.redisPort
  lazy val redis = wire[ConfiguredRedis]

  lazy val api = routes

  val httpBindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(handler = api, interface = Config.host, port = Config.port)

  httpBindingFuture.onComplete {
    case Success(_) =>
      logger.info(s"REST interface bound to http://${Config.host}:${Config.port}")

    case Failure(ex) =>
      logger.error(s"REST interface could not bind to ${Config.host}:${Config.port}", ex.getMessage)
      system.terminate()
  }

  startSqsMessageConsumer

  sys.addShutdownHook {
    logger.info("Killing app ordering gracefully")
    Await.result(httpBindingFuture.map(_.unbind()), Duration.Inf)
    Await.result(system.terminate(), Duration.Inf)
    logger.info("App ordering is now off...BYE BYE!")
  }

}
