package io.paytouch.ordering.utils

import io.paytouch.ordering.data.db.DatabaseProvider
import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._
import io.paytouch.ordering.utils.db.ConfigurableDatabaseProvider

trait ConfiguredTestDatabase extends DatabaseProvider {
  implicit lazy val db: Database = ConfiguredTestDatabaseProvider.db
}

object ConfiguredTestDatabaseProvider extends ConfigurableDatabaseProvider {
  lazy val configFile = "unitTests.conf"
}

class ConfiguredDatabaseProviderShutdown {
  ConfiguredTestDatabaseProvider.db.close()
}
