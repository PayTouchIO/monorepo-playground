package io.paytouch.ordering.data.db

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._

trait DatabaseProvider {
  def db: Database
}

trait ConfiguredDatabase extends DatabaseProvider {
  lazy val db = ConfiguredDatabase.db
}

object ConfiguredDatabase extends DatabaseConfiguration {

  lazy val db: Database = dbConfig("postgres")
}

trait DatabaseConfiguration {

  def dbConfig(key: String): Database = {
    val database = Database.forConfig(key)
    // Currency issue of the DriverDataSource in initialization  -- see https://github.com/slick/slick/issues/1400
    // adding workaround ro force first session
    val session = database.createSession()
    try session.force()
    finally session.close()
    database
  }
}
