package io.paytouch.ordering.services

import akka.testkit.TestProbe
import io.paytouch.ordering.utils.{ DaoSpec, MockedRestApi, MultipleLocationFixtures, ValidatedHelpers }

abstract class ServiceDaoSpec extends DaoSpec {

  abstract class ServiceDaoSpecContext
      extends ServiceDaoSpecBaseContext
         with MultipleLocationFixtures
         with ValidatedHelpers {
    implicit val userCtx = userContext
  }

  abstract class ServiceDaoSpecBaseContext extends DaoSpecContext {
    val actorSystem = MockedRestApi.testAsyncSystem
    val actorMock = new TestProbe(actorSystem)

    lazy val ptCoreClient = MockedRestApi.ptCoreClient
    val merchantService = MockedRestApi.merchantService
  }
}
