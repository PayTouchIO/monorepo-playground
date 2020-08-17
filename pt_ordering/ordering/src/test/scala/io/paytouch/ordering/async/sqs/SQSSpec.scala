package io.paytouch.ordering.async.sqs

import io.paytouch.ordering.utils.{ CommonArbitraries, FSpec, MockedRestApi }

import org.specs2.mock.Mockito

abstract class SQSSpec extends FSpec with Mockito with CommonArbitraries {

  lazy val senderSystem = MockedRestApi.testAsyncSystem

  abstract class SQSSpecContext extends FSpecContext
}
