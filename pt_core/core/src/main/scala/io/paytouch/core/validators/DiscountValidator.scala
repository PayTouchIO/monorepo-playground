package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, DiscountDao }
import io.paytouch.core.data.model.DiscountRecord
import io.paytouch.core.errors.{ InvalidDiscountIds, NonAccessibleDiscountIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class DiscountValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[DiscountRecord] {

  type Record = DiscountRecord
  type Dao = DiscountDao

  protected val dao = daos.discountDao
  val validationErrorF = InvalidDiscountIds(_)
  val accessErrorF = NonAccessibleDiscountIds(_)

}
