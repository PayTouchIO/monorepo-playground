package io.paytouch.core.validators.features

import java.util.UUID

import io.paytouch.core.data.daos.features.SlickDao
import io.paytouch.core.data.model.{ SlickMerchantRecord, SlickOneToOneWithLocationRecord }
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors

trait DefaultValidatorWithLocation[R <: SlickMerchantRecord with SlickOneToOneWithLocationRecord]
    extends DefaultValidator[R] {

  type Dao <: SlickDao { type Record = R }

  protected def dao: Dao
  val validationErrorF: Seq[UUID] => errors.Error
  val accessErrorF: Seq[UUID] => errors.Error

  override def validityCheck(record: R)(implicit user: UserContext): Boolean =
    record.merchantId == user.merchantId && user.locationIds.contains(record.locationId)
}
