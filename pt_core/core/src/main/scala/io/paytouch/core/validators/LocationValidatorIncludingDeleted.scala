package io.paytouch.core.validators

import io.paytouch.core.data.daos.{ Daos, LocationDao }
import io.paytouch.core.data.model.LocationRecord
import io.paytouch.core.errors.{ InvalidLocationIds, NonAccessibleLocationIds }
import io.paytouch.core.validators.features.DefaultValidatorIncludingDeleted

import scala.concurrent.ExecutionContext

class LocationValidatorIncludingDeleted(implicit val ec: ExecutionContext, val daos: Daos)
    extends DefaultValidatorIncludingDeleted[LocationRecord] {

  type Record = LocationRecord
  type Dao = LocationDao

  protected val dao = daos.locationDao
  val validationErrorF = InvalidLocationIds(_)
  val accessErrorF = NonAccessibleLocationIds(_)

}
