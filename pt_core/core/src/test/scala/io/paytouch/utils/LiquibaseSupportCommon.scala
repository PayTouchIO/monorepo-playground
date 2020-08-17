package io.paytouch.utils

import java.io.File
import java.sql.{ Connection, DriverManager }
import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }
import com.typesafe.scalalogging.LazyLogging
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.FileSystemResourceAccessor
import scala.util.{ Failure, Success, Try }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._

trait LiquibaseSupportCommon extends LazyLogging {
  val dbHelper: DbHelper

  require(
    dbHelper.databaseName.contains("test"),
    "STOP! Your database name should be clearly marked for test purposes only. " +
      s"You must include 'test' in your db name as a security measure. ${dbHelper.databaseName} is not a valid database name",
  )

  private def run(): Unit = {
    dbHelper.createIfNeeded()

    implicit val connection: Connection =
      dbHelper.openLiquibaseConnectionToDb()

    try dbHelper.update
    finally connection.close
  }

  run()
}

object ChangeLog {
  val changelog: String =
    getClass.getResource("/migrations/changelog.yml").getFile
}

class DbHelper(val databaseName: String, configFile: String) extends LazyLogging with TestExecutionContext {

  private val baseConfig = ConfigFactory.load(configFile)
  private val originalUrl = baseConfig.getString("postgres.url")
  private def replaceDbName(targetDb: String) = (originalUrl.split("/").dropRight(1) ++ Seq(targetDb)).mkString("/")
  private val dbUrl = replaceDbName(databaseName)
  private val maintenanceUrl = replaceDbName("postgres")

  private lazy val config =
    baseConfig.withValue("postgres.url", ConfigValueFactory.fromAnyRef(dbUrl))

  lazy val user = config.getString("postgres.user")
  lazy val password = config.getString("postgres.password")

  lazy val skipRebuild = config.getBoolean("postgres.skipRebuild")

  lazy val db = Database.forConfig(path = "postgres", config)

  private def liquibase(implicit connection: Connection) = {
    val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection))
    new Liquibase(ChangeLog.changelog, new FileSystemResourceAccessor(), database)
  }

  def openLiquibaseConnectionToDb(): Connection =
    db.source.createConnection()

  def createIfNeeded(): Unit =
    Try {
      dropAllIfNeeded()

      logger.info(s"Creating $databaseName")
      runMaintenanceStatement(s"CREATE DATABASE $databaseName")
    } match {
      case Success(_) =>
        logger.info(s"Database $databaseName has been created")

      case Failure(ex) if ex.getMessage.contains("already exists") =>
        logger.info(s"Database $databaseName not created as it already exists")

      case Failure(ex) =>
        logger.error(ex.getMessage, ex)
    }

  def dropAllIfNeeded(): Unit =
    if (skipRebuild)
      ()
    else {
      logger.info(s"Dropping $databaseName")

      Try(runMaintenanceStatement(s"DROP DATABASE $databaseName")) match {
        case Success(_) =>
          logger.info(s"Database $databaseName has been dropped")

        case Failure(ex) =>
          logger.error(ex.getMessage, ex)
      }
    }

  def update(implicit connection: Connection): Unit = {
    logger.info("Running all migrations")

    liquibase.update("")
  }

  private def runMaintenanceStatement(sql: String): Unit = {
    val maintenanceUrl = replaceDbName("postgres")
    val user = config.getString("postgres.user")
    val password = config.getString("postgres.password")
    val driver = config.getString("postgres.driver")
    Class.forName(driver)
    implicit val connection = DriverManager.getConnection(maintenanceUrl, user, password)
    try runStatement(sql)
    finally connection.close
  }

  private def runStatement(sql: String)(implicit connection: Connection): Unit = {
    val statement = connection.createStatement
    statement.executeUpdate(sql)
    statement.close
  }
}
