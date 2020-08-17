package io.paytouch.ordering.utils.db

trait LiquibaseSupportCommon {
  private lazy val dbHelper = new DbHelper(configFile)
  def databaseName: String = dbHelper.databaseName
  def configFile: String

  require(
    databaseName.contains("test"),
    "STOP! Your database name should be clearly marked for test purposes only. " +
      s"You must include 'test' in your db name as a security measure. $databaseName is not a valid database name",
  )

  private def run(): Unit = {
    dbHelper.createIfNeeded
    implicit val connection = dbHelper.openLiquibaseConnectionToDb
    try {
      dbHelper.dropAll
      dbHelper.update
    }
    finally connection.close
  }

  run()
}
