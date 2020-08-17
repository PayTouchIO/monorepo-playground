package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ CashDrawerDao, Daos }
import io.paytouch.core.data.model.CashDrawerRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.errors.{ InvalidCashDrawerIds, NonAccessibleCashDrawerIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class CashDrawerValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[CashDrawerRecord] {

  type Record = CashDrawerRecord
  type Dao = CashDrawerDao

  protected val dao = daos.cashDrawerDao
  val validationErrorF = InvalidCashDrawerIds(_)
  val accessErrorF = NonAccessibleCashDrawerIds(_)

  override def validityCheck(record: Record)(implicit user: UserContext): Boolean =
    record.merchantId == user.merchantId && record.locationId.forall(user.locationIds.contains)
}
