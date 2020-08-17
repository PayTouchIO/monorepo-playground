package io.paytouch.core

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core.{ ServiceConfigurations => Config }
import io.sentry.Sentry

object Main extends PtCore with LazyLogging {
  Sentry.init

  startSqsMessageConsumer

  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(handler = api, interface = Config.host, port = Config.port)

  bindingFuture.onComplete {
    case Success(_) =>
      logger.info(s"REST interface bound to http://${Config.host}:${Config.port}")

    case Failure(ex) =>
      logger.error(s"REST interface could not bind to ${Config.host}:${Config.port}", ex.getMessage)
      system.terminate()
  }

  sys.addShutdownHook {
    logger.info("Killing app core gracefully")
    Await.result(bindingFuture.map(_.unbind()), Duration.Inf)
    Await.result(system.terminate(), Duration.Inf)
    logger.info("App core is now off...BYE BYE!")
  }
}
