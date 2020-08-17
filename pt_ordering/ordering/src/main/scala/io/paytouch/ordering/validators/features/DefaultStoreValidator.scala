package io.paytouch.ordering.validators.features

import java.util.UUID

import io.paytouch.ordering.data.daos.features.SlickDao
import io.paytouch.ordering.data.model.SlickStoreRecord
import io.paytouch.ordering.entities.StoreContext

import scala.concurrent.Future

trait DefaultStoreValidator extends Validator with DeletionValidator { self =>

  type Context = StoreContext
  type Dao <: SlickDao { type Record = self.Record }
  type Record <: SlickStoreRecord

  protected def dao: Dao

  protected def recordsFinder(ids: Seq[UUID])(implicit store: StoreContext): Future[Seq[Record]] = dao.findByIds(ids)

  protected def validityCheck(record: Record)(implicit store: StoreContext): Boolean =
    record.storeId == store.id

}
