package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ BrandDao, Daos }
import io.paytouch.core.data.model.BrandRecord
import io.paytouch.core.errors.{ InvalidBrandIds, NonAccessibleBrandIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class BrandValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[BrandRecord] {

  type Record = BrandRecord
  type Dao = BrandDao

  protected val dao = daos.brandDao
  val validationErrorF = InvalidBrandIds(_)
  val accessErrorF = NonAccessibleBrandIds(_)
}
