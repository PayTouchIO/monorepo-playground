package io.paytouch.core.services.features

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickOrderItemDao
import io.paytouch.core.data.model.{ SlickOrderItemRelationRecord, SlickRecord }
import io.paytouch.core.entities.{ ExposedEntity, UserContext }
import io.paytouch.core.utils.Implicits

import scala.concurrent._

trait OrderItemRelationService extends Implicits { self =>
  type Dao <: SlickOrderItemDao { type Record = self.Record }
  type Entity <: ExposedEntity
  type Record <: SlickRecord with SlickOrderItemRelationRecord

  protected def dao: Dao

  def findAllItemsByOrderItemIds(orderItemIds: Seq[UUID])(implicit user: UserContext): Future[Map[UUID, Seq[Entity]]] =
    dao
      .findAllByOrderItemIds(orderItemIds)
      .map(records => records.groupBy(_.orderItemId).transform((_, v) => toSeqEntity(v)))

  def fromRecordToEntity(record: Record)(implicit user: UserContext): Entity

  private def toSeqEntity(s: Seq[Record])(implicit user: UserContext): Seq[Entity] = s.map(fromRecordToEntity)
}
