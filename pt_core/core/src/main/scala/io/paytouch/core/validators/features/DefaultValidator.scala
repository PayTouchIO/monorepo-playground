package io.paytouch.core.validators.features

import scala.concurrent._

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickDao
import io.paytouch.core.data.model.SlickMerchantRecord
import io.paytouch.core.entities.UserContext

trait DefaultValidator[R <: SlickMerchantRecord] extends Validator[R] with DeletionValidator[R] { self =>
  type Dao <: SlickDao { type Record = R }

  protected def dao: Dao

  protected def recordsFinder(ids: Seq[UUID])(implicit user: UserContext): Future[Seq[R]] = dao.findByIds(ids)
}
