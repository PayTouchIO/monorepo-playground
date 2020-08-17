package io.paytouch.ordering.utils.db

import java.sql.{ Connection, DriverManager }

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.ordering.utils.TestExecutionContext
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.FileSystemResourceAccessor

import scala.util.{ Failure, Success, Try }

class DbHelper(configFile: String) extends LazyLogging with TestExecutionContext {

  private lazy val config = ConfigFactory.load(configFile)

  lazy val user = config.getString("postgres.user")
  lazy val password = config.getString("postgres.password")
  lazy val driver = config.getString("postgres.driver")

  lazy val skipRebuild = config.getBoolean("postgres.skipRebuild")

  private def liquibase(implicit connection: Connection) = {
    Class.forName(driver)
    val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection))
    new Liquibase(ChangeLog.changelog, new FileSystemResourceAccessor(), database)
  }

  private lazy val server = config.getString("postgres.url")
  lazy val (serverBaseUrl, databaseName) = {
    val parts = server.split("/")
    val baseUrl = parts.dropRight(1).mkString("/")
    val dbName = parts.last

    baseUrl -> dbName
  }

  lazy val testUri = server
  private lazy val maintenanceUri = s"$serverBaseUrl/postgres"

  def openLiquibaseConnectionToDb = {
    Class.forName(driver)
    DriverManager.getConnection(testUri, user, password)
  }

  def createIfNeeded =
    Try(runMaintenanceStatement(s"CREATE DATABASE $databaseName")) match {
      case Success(_) => logger.info(s"Database $databaseName has been created")
      case Failure(ex) if ex.getMessage.contains("already exists") =>
        logger.info(s"Database $databaseName not created as it already exists")
      case Failure(ex) => logger.error(ex.getMessage, ex)
    }

  def dropAll(implicit connection: Connection) =
    if (!skipRebuild) {
      dropFunctionsAndViews
      liquibase.dropAll
    }

  def update(implicit connection: Connection) = liquibase.update("")

  private def dropFunctionsAndViews = {
    val dropStatements = Seq()
    dropStatements.map(runDbStatement)
  }

  private def runMaintenanceStatement(sql: String) = runStatement(maintenanceUri, sql)

  private def runDbStatement(sql: String) = runStatement(testUri, sql)

  private def runStatement(uri: String, sql: String) = {
    Class.forName(driver)
    val serverConnection = DriverManager.getConnection(uri, user, password)
    try {
      val statement = serverConnection.createStatement
      statement.executeUpdate(sql)
      statement.close
    }
    finally serverConnection.close
  }
}
