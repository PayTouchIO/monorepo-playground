package io.paytouch.core.reports.validators

import io.paytouch.core.data.daos.Daos
import io.paytouch.core.data.model.LocationRecord
import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.filters.ReportFilters
import io.paytouch.core.utils.Multiple.ErrorsOr
import io.paytouch.core.validators.LocationValidator

import scala.concurrent._

class FiltersValidator(implicit val ec: ExecutionContext, val daos: Daos) {

  val locationValidator = new LocationValidator

  def validateFilters[Filters <: ReportFilters](
      filters: Filters,
    )(implicit
      user: UserContext,
    ): Future[ErrorsOr[Seq[LocationRecord]]] =
    locationValidator.accessByIds(filters.locationIds)
}
