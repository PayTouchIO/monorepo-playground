package io.paytouch.core.data.db

import io.paytouch.core.data.driver.CustomPostgresDriver.api._

trait DatabaseProvider {
  def db: Database
}

trait ConfiguredDatabase extends DatabaseProvider {
  lazy val db = ConfiguredDatabase.db
}

object ConfiguredDatabase {
  lazy val db: Database = Database.forConfig("postgres")
}

object SlowOpsDatabase {
  lazy val db: Database = Database.forConfig("reports.postgres")
}
