package io.paytouch.core.utils

import scala.concurrent.duration._

import akka.actor.ActorSystem

import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.testkit._
import akka.http.scaladsl.settings.RoutingSettings

trait PaytouchRouteSpec extends PaytouchSpec with TestFrameworkInterface with RouteTest {
  override def createActorSystem(): ActorSystem =
    MockedRestApi.testAsyncSystem

  implicit val timeout: RouteTestTimeout =
    RouteTestTimeout(45.seconds)

  override def failTest(msg: String) =
    throw new Exception(msg)

  override def testExceptionHandler: ExceptionHandler =
    ExceptionHandler.default(RoutingSettings.default)
}
