package io.paytouch.core.async.monitors

import io.paytouch.core.utils.{ FSpec, MockedRestApi, MultipleLocationFixtures }

abstract class MonitorSpec extends FSpec {

  lazy val monitorSystem = MockedRestApi.testAsyncSystem

  abstract class MonitorSpecContext extends FSpecContext

  trait MonitorStateFixtures extends MultipleLocationFixtures
}
