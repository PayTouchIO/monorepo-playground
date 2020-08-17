package io.paytouch.core.async.importers.parsers

import akka.actor.Props
import io.paytouch.core.async.importers.loaders.ProductImportLoader
import io.paytouch.core.async.trackers.EventTracker
import io.paytouch.core.data.daos.{ ConfiguredTestDatabase, DaoSpec }
import io.paytouch.core.entities.UserContext
import io.paytouch.utils.Tagging._
import io.paytouch.core.utils.{ MockedRestApi, ValidatedHelpers, FixtureDaoFactory => Factory }
import io.paytouch.utils.TestExecutionContext
import org.specs2.specification.Scope

abstract class ParserSpec extends DaoSpec { self =>
  val resources: String =
    getClass.getClassLoader.getResource("").getFile

  abstract class ParserSpecContext
      extends Scope
         with TestExecutionContext
         with ConfiguredTestDatabase
         with ValidatedHelpers {

    implicit val daos = self.daos
    val productDao = daos.productDao
    val productLocationDao = daos.productLocationDao
    val stockDao = daos.stockDao

    val testAsyncSystem = MockedRestApi.testAsyncSystem
    val eventTracker = testAsyncSystem.actorOf(Props(new EventTracker)).taggedWith[EventTracker]

    val parser = new ProductImportParser
    val loader = new ProductImportLoader(eventTracker, MockedRestApi.setupStepService)

    val merchant = Factory.merchant.create
    val location = Factory.location(merchant).create
    val locations = Seq(location)
    val defaultMenu = Factory.defaultMenuCatalog(merchant).create

    @scala.annotation.nowarn("msg=Auto-application")
    implicit lazy val userContext = random[UserContext].copy(merchantId = merchant.id, locationIds = Seq(location.id))
    val articleService = MockedRestApi.articleService
  }
}
