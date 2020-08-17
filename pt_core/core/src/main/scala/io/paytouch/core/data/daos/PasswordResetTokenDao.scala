package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model._
import io.paytouch.core.data.tables.PasswordResetTokensTable
import io.paytouch.core.utils.UtcTime

class PasswordResetTokenDao(implicit val ec: ExecutionContext, val db: Database) extends SlickDao {
  type Record = PasswordResetTokenRecord
  type Update = PasswordResetTokenUpdate
  type Table = PasswordResetTokensTable

  val table = TableQuery[Table]

  def findByUserIdAndToken(userId: UUID, token: String): Future[Option[Record]] =
    table
      .filter(_.userId === userId)
      .filter(_.key === token)
      .filter(_.expiresAt > UtcTime.now)
      .result
      .headOption
      .pipe(run)

  def deleteAllByUserId(userId: UUID): Future[UUID] =
    table
      .filter(_.userId === userId)
      .delete
      .map(_ => userId)
      .pipe(run)
}
