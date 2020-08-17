package io.paytouch.core.processors

import akka.testkit.TestProbe
import io.paytouch.core.data.daos.{ ConfiguredTestDatabase, Daos }
import io.paytouch.core.utils._
import org.specs2.specification.Scope

abstract class ProcessorSpec extends PaytouchSpec with LiquibaseSupportProvider with ConfiguredTestDatabase {
  implicit lazy val daos = new Daos

  abstract class ProcessorSpecContext extends Scope with ValidatedHelpers {
    val actorSystem = MockedRestApi.testAsyncSystem
    val actorMock = new TestProbe(actorSystem)
  }

}
