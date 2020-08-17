package io.paytouch.core.async.sqs

import io.paytouch.core.utils.{ FSpec, MockedRestApi }

abstract class SQSSpec extends FSpec {

  lazy val senderSystem = MockedRestApi.testAsyncSystem

  abstract class SQSSpecContext extends FSpecContext
}
