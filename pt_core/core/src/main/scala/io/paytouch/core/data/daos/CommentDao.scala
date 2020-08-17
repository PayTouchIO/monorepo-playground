package io.paytouch.core.data.daos

import java.util.UUID

import io.paytouch.core.data.daos.features.{ SlickDefaultUpsertDao, SlickFindAllDao, SlickMerchantDao }
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.enums.CommentType
import io.paytouch.core.data.model.{ CommentRecord, CommentUpdate }
import io.paytouch.core.data.tables.CommentsTable
import io.paytouch.core.filters.CommentFilters

import scala.concurrent._

class CommentDao(implicit val ec: ExecutionContext, val db: Database)
    extends SlickMerchantDao
       with SlickFindAllDao
       with SlickDefaultUpsertDao {

  type Record = CommentRecord
  type Update = CommentUpdate
  type Table = CommentsTable
  type Filters = CommentFilters

  val table = TableQuery[Table]

  def findAllWithFilters(merchantId: UUID, filters: Filters)(offset: Int, limit: Int): Future[Seq[Record]] =
    run(queryFindAll(merchantId, filters.`type`, filters.objectId).drop(offset).take(limit).result)

  def countAllWithFilters(merchantId: UUID, filters: Filters): Future[Int] =
    run(queryFindAll(merchantId, filters.`type`, filters.objectId).length.result)

  def queryFindAll(
      merchantId: UUID,
      objectType: Option[CommentType],
      objectId: Option[UUID],
    ) =
    queryFindAllByMerchantId(merchantId)
      .filter(t =>
        all(
          objectType.map(ot => t.objectType === ot),
          objectId.map(oi => t.objectId === oi),
        ),
      )
      .sortBy(_.createdAt.asc)

}
