package io.paytouch.core.data.daos

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.daos.features.SlickDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ AdminRecord, AdminUpdate }
import io.paytouch.core.data.tables.AdminsTable
import io.paytouch.core.entities.AdminLogin
import io.paytouch.core.utils.UtcTime

import scala.concurrent._

class AdminDao(implicit val ec: ExecutionContext, val db: Database) extends SlickDao {

  type Record = AdminRecord
  type Update = AdminUpdate
  type Table = AdminsTable

  val table = TableQuery[Table]

  def findAll(offset: Int, limit: Int): Future[Seq[Record]] = run(table.drop(offset).take(limit).result)

  def findByEmail(email: String): Future[Option[Record]] = {
    val q = table.filter(_.email === email)
    run(q.result.headOption)
  }

  def findAdminLoginByEmail(email: String): Future[Option[AdminLogin]] = {
    val q = table.filter(_.email === email).map(_.adminLogin)
    run(q.result.headOption)
  }

  def recordLastLogin(id: UUID, date: ZonedDateTime): Future[Boolean] = {
    val field = for { o <- table if o.id === id } yield (o.lastLoginAt, o.updatedAt)
    run(field.update(Some(date), UtcTime.now).map(_ > 0))
  }
}
