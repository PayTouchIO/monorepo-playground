package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, TaxRateDao }
import io.paytouch.core.data.model.TaxRateRecord
import io.paytouch.core.errors._
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class TaxRateValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[TaxRateRecord] {

  type Record = TaxRateRecord
  type Dao = TaxRateDao

  protected val dao = daos.taxRateDao
  val validationErrorF = InvalidTaxRateIds(_)
  val accessErrorF = NonAccessibleTaxRateIds(_)

}
