package io.paytouch.ordering.utils.db

import io.paytouch.ordering.data.driver.PaytouchPostgresDriver.api._

trait ConfigurableDatabaseProvider {
  private lazy val dbHelper = new DbHelper(configFile)

  def databaseName: String = dbHelper.databaseName
  def configFile: String

  Class.forName(dbHelper.driver)

  lazy val db: Database = {
    dbHelper.createIfNeeded
    val database = Database.forURL(dbHelper.testUri, dbHelper.user, dbHelper.password)
    // Currency issue of the DriverDataSource in initialization  -- see https://github.com/slick/slick/issues/1400
    // adding workaround to force first session
    val session = database.createSession()
    try session.force()
    finally session.close()
    database
  }
}
