package io.paytouch.core.data.daos

import java.util.UUID

import scala.concurrent._

import io.paytouch.implicits._

import io.paytouch.core.data.daos.features.SlickMerchantDao
import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.{ ModifierOptionRecord, ModifierOptionUpdate }
import io.paytouch.core.data.tables.ModifierOptionsTable

class ModifierOptionDao(implicit val ec: ExecutionContext, val db: Database) extends SlickMerchantDao {
  type Record = ModifierOptionRecord
  type Update = ModifierOptionUpdate
  type Table = ModifierOptionsTable

  val table = TableQuery[Table]

  def queryFindByModifierSetIds(modifierSetIds: Seq[UUID]) =
    table.filter(_.modifierSetId inSet modifierSetIds)

  def findByModifierSetIds(modifierSetIds: Seq[UUID]): Future[Seq[Record]] =
    if (modifierSetIds.isEmpty)
      Future.successful(Seq.empty)
    else
      queryFindByModifierSetIds(modifierSetIds)
        .sortBy(mo => (mo.position.asc, mo.name.asc))
        .result
        .pipe(run)

  def findByModifierSetId(modifierSetId: UUID): Future[Seq[Record]] =
    findByModifierSetIds(Seq(modifierSetId))

  def queryBulkUpsertAndDeleteTheRestByModifierSetId(modifierOptions: Seq[Update], modifierSetId: UUID) =
    for {
      us <- queryBulkUpsert(modifierOptions)
      records = us.map { case (_, record) => record }
      _ <- queryDeleteTheRestByDeleteFilter(records, _.modifierSetId === modifierSetId)
    } yield records
}
