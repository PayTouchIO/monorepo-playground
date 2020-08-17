package io.paytouch.core.validators.features

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickSoftDeleteDao
import io.paytouch.core.data.model.SlickSoftDeleteRecord
import io.paytouch.core.entities.UserContext

import scala.concurrent._

trait DefaultValidatorIncludingDeleted[R <: SlickSoftDeleteRecord] extends Validator[R] {

  type Dao <: SlickSoftDeleteDao { type Record = R }

  protected def dao: Dao

  protected def recordsFinder(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[R]] =
    dao.findDeletedByIds(ids)
}
