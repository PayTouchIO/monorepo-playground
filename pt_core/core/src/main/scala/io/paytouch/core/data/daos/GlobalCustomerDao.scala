package io.paytouch.core.data.daos

import io.paytouch.core.data.daos.features.SlickDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ GlobalCustomerRecord, GlobalCustomerUpdate }
import io.paytouch.core.data.tables.GlobalCustomersTable

import scala.concurrent._

class GlobalCustomerDao(implicit val ec: ExecutionContext, val db: Database) extends SlickDao {

  type Record = GlobalCustomerRecord
  type Update = GlobalCustomerUpdate
  type Table = GlobalCustomersTable

  val table = TableQuery[Table]

  def queryFindByEmail(email: Option[String]) = table.filter(_.email === email && email.isDefined).result.headOption

  def findByEmail(email: Option[String]): Future[Option[GlobalCustomerRecord]] =
    run(queryFindByEmail(email))
}
