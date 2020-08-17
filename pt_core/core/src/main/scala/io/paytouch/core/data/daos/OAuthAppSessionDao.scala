package io.paytouch.core.data.daos

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OAuthAppSessionRecord, OAuthAppSessionUpdate }
import io.paytouch.core.data.tables.OAuthAppSessionsTable

import scala.concurrent.ExecutionContext

class OAuthAppSessionDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {

  type Record = OAuthAppSessionRecord
  type Update = OAuthAppSessionUpdate
  type Table = OAuthAppSessionsTable

  val table = TableQuery[Table]

}
