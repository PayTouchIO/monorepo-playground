package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OAuthAppRecord, OAuthAppUpdate }
import io.paytouch.core.data.tables.OAuthAppsTable

import scala.concurrent._

class OAuthAppDao(implicit val ec: ExecutionContext, val db: Database) extends SlickDao {

  type Record = OAuthAppRecord
  type Update = OAuthAppUpdate
  type Table = OAuthAppsTable

  val table = TableQuery[Table]

  def findOneByClientId(clientId: UUID): Future[Option[Record]] =
    run(table.filter(_.clientId === clientId).result.headOption)

  def queryFindByName(name: String) =
    table.filter(_.name.toLowerCase === name.toLowerCase)
}
