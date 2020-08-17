package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickDefaultUpsertDao, SlickFindAllDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ UserRoleRecord, UserRoleUpdate }
import io.paytouch.core.data.tables.UserRolesTable
import io.paytouch.core.filters.NoFilters

import scala.concurrent._

class UserRoleDao(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickFindAllDao
       with SlickDefaultUpsertDao {
  type Record = UserRoleRecord
  type Update = UserRoleUpdate
  type Filters = NoFilters
  type Table = UserRolesTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, f: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    run(queryFindAllByMerchantId(merchantId = merchantId).drop(offset).take(limit).result)

  def countAllWithFilters(merchantId: UUID, f: Filters): Future[Int] =
    run(queryFindAllByMerchantId(merchantId = merchantId).length.result)
}
