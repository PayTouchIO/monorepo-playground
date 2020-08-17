package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ BundleOptionDao, Daos }
import io.paytouch.core.data.model.BundleOptionRecord
import io.paytouch.core.errors.{ InvalidBundleOptionIds, NonAccessibleBundleOptionIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class BundleOptionValidator(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidator[BundleOptionRecord] {

  type Record = BundleOptionRecord
  type Dao = BundleOptionDao

  protected val dao = daos.bundleOptionDao
  val validationErrorF = InvalidBundleOptionIds(_)
  val accessErrorF = NonAccessibleBundleOptionIds(_)
}
