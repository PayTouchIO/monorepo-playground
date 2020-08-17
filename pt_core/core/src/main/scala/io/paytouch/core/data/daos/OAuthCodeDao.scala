package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ OAuthCodeRecord, OAuthCodeUpdate }
import io.paytouch.core.data.tables.OAuthCodesTable

import scala.concurrent._

class OAuthCodeDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {

  type Record = OAuthCodeRecord
  type Update = OAuthCodeUpdate
  type Table = OAuthCodesTable

  val table = TableQuery[Table]

  def findOneByAppIdAndCode(oauthAppId: UUID, code: UUID): Future[Option[Record]] =
    run(table.filter(_.oauthAppId === oauthAppId).filter(_.code === code).result.headOption)

}
