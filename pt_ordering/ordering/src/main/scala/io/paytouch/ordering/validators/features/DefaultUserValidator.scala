package io.paytouch.ordering.validators.features

import java.util.UUID

import io.paytouch.ordering.data.daos.features.SlickDao
import io.paytouch.ordering.data.model.SlickLocationRecord
import io.paytouch.ordering.entities.UserContext

import scala.concurrent.Future

trait DefaultUserValidator extends Validator with DeletionValidator { self =>

  type Context = UserContext
  type Dao <: SlickDao { type Record = self.Record }
  type Record <: SlickLocationRecord

  protected def dao: Dao

  protected def recordsFinder(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[Record]] = dao.findByIds(ids)

  protected def validityCheck(record: Record)(implicit user: UserContext): Boolean =
    user.locationIds.contains(record.locationId)

}
