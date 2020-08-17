package io.paytouch.core.data.daos

import io.paytouch.core.data.db.DatabaseProvider
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.utils.{ LiquibaseSupportProvider, PaytouchSpec }
import io.paytouch.utils.{ DbHelper, TestExecutionContext }
import org.specs2.specification.Scope

abstract class DaoSpec extends PaytouchSpec with LiquibaseSupportProvider with ConfiguredTestDatabase {

  implicit lazy val daos = new Daos

  abstract class DaoSpecContext extends TestExecutionContext with Scope
}

trait ConfiguredTestDatabase extends DatabaseProvider {
  implicit lazy val db: Database = ConfiguredDatabaseProvider.db
}

object ConfiguredDatabaseProvider {
  lazy val dbHelper = new DbHelper("unit_tests", "unitTests.conf")
  lazy val db: Database = dbHelper.db
}

class ConfiguredDatabaseProviderShutdown {
  ConfiguredDatabaseProvider.db.close()
}
