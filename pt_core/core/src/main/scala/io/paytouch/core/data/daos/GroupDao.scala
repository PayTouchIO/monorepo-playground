package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickFindAllDao, SlickMerchantDao, SlickUpsertDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.upsertions.GroupUpsertion
import io.paytouch.core.data.model.{ GroupRecord, GroupUpdate }
import io.paytouch.core.data.tables.GroupsTable
import io.paytouch.core.filters.GroupFilters
import io.paytouch.core.utils.ResultType

import scala.concurrent._

class GroupDao(val customerGroupDao: CustomerGroupDao)(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickFindAllDao
       with SlickUpsertDao {

  type Record = GroupRecord
  type Update = GroupUpdate
  type Upsertion = GroupUpsertion
  type Filters = GroupFilters
  type Table = GroupsTable

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, filters: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    run(queryFindAllByMerchantId(merchantId, filters.query).drop(offset).take(limit).result)

  def countAllWithFilters(merchantId: UUID, filters: Filters): Future[Int] =
    run(queryFindAllByMerchantId(merchantId, filters.query).length.result)

  def queryFindAllByMerchantId(merchantId: UUID, query: Option[String] = None) =
    table
      .filter(t =>
        all(
          Some(t.merchantId === merchantId),
          query.map(q => t.name.toLowerCase like s"%${q.toLowerCase}%"),
        ),
      )
      .sortBy(_.name.asc)

  def upsert(upsertion: Upsertion): Future[(ResultType, Record)] = {
    val upserts = for {
      (resultType, group) <- queryUpsert(upsertion.groupUpdate)
      customerGroups <- asOption(upsertion.customerGroupUpdates.map { customerGroupUpdates =>
        customerGroupDao.queryBulkUpsertAndDeleteTheRestByGroupId(customerGroupUpdates, group.id)
      })
    } yield (resultType, group)
    runWithTransaction(upserts)
  }

}
