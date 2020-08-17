package io.paytouch.ordering.processors

import akka.testkit.TestProbe
import io.paytouch.ordering.data.daos.Daos
import io.paytouch.ordering.utils._
import org.specs2.specification.Scope

abstract class ProcessorSpec extends PaytouchSpec with LiquibaseSupportProvider with ConfiguredTestDatabase {
  implicit lazy val daos = new Daos

  abstract class ProcessorSpecContext extends Scope with ValidatedHelpers {
    val actorSystem = MockedRestApi.testAsyncSystem
    val actorMock = new TestProbe(actorSystem)
  }

}
