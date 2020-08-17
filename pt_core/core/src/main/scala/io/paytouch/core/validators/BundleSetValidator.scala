package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ BundleSetDao, Daos }
import io.paytouch.core.data.model.BundleSetRecord
import io.paytouch.core.errors.{ InvalidBundleSetIds, NonAccessibleBundleSetIds }
import io.paytouch.core.validators.features.DefaultValidator

import scala.concurrent.ExecutionContext

class BundleSetValidator(implicit val ec: ExecutionContext, val daos: Daos) extends DefaultValidator[BundleSetRecord] {

  type Record = BundleSetRecord
  type Dao = BundleSetDao

  protected val dao = daos.bundleSetDao
  val validationErrorF = InvalidBundleSetIds(_)
  val accessErrorF = NonAccessibleBundleSetIds(_)

}
