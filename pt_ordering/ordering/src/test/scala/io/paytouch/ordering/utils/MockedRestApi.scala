package io.paytouch.ordering
package utils

import akka.actor._
import akka.stream.Materializer
import akka.testkit.TestProbe

import com.softwaremill.macwire._
import com.softwaremill.sttp.testing.SttpBackendStub

import io.paytouch.logging.BaseMdcActor
import io.paytouch.ordering.async.sqs.SQSMessageSender
import io.paytouch.ordering.data.redis._
import io.paytouch.ordering.messages.SQSMessageHandler
import io.paytouch.ordering.stubs._

object MockedRestApi extends RestApi with ConfiguredTestDatabase {
  implicit lazy val testAsyncSystem = ActorSystem("unit-test-async")
  implicit lazy val materializer = Materializer(testAsyncSystem)
  implicit lazy val ec = testAsyncSystem.dispatcher
  lazy val realMdcActor = mdcActor

  lazy val AppHeaderName = ServiceConfigurations.AppHeaderName
  lazy val VersionHeaderName = ServiceConfigurations.VersionHeaderName
  lazy val corsAllowOrigins = ServiceConfigurations.allowOrigins

  lazy val asyncSystem = testAsyncSystem

  lazy val mockMessageSenderProbe = new TestProbe(testAsyncSystem)

  override lazy val messageSender: ActorRef withTag SQSMessageSender =
    mockMessageSenderProbe.ref.taggedWith[SQSMessageSender]

  override lazy val messageHandler = new SQSMessageHandler(asyncSystem, messageSender)

  override implicit lazy val mdcActor: ActorRef withTag BaseMdcActor =
    mockMessageSenderProbe.ref.taggedWith[BaseMdcActor]

  lazy val ptCoreClient = new PtCoreStubClient
  lazy val gMapsClient = new GMapsStubClient
  lazy val stripeClient = new StripeStubClient

  implicit lazy val sttpBackend =
    SttpBackendStub.synchronous.whenRequestMatches(_ => true).thenRespond("Real HTTP connections are disabled in tests")
  lazy val worldpayClient = wire[WorldpayStubClient]

  private lazy val redisHost = ServiceConfigurations.redisHost
  private lazy val redisPort = ServiceConfigurations.redisPort
  lazy val redis: ConfiguredRedis = new ConfiguredRedis(redisHost, redisPort)
}
