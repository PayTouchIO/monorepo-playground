package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.entities._
import io.paytouch.core.expansions.NoExpansions
import io.paytouch.core.filters.PayrollFilters
import io.paytouch.core.services.PayrollService

trait PayrollResource extends JsonResource {

  def payrollService: PayrollService

  val payrollRoutes: Route = path("payroll.list") {
    get {
      paginateWithDefaults(30) { implicit pagination: Pagination =>
        parameters(
          "location_id".as[UUID].?,
          "from".as[LocalDateTime].?,
          "to".as[LocalDateTime].?,
          "q".?,
        ) {
          case (locationId, from, to, query) =>
            authenticate { implicit user =>
              val filters = PayrollFilters.withAccessibleLocations(locationId, from, to, query)
              onSuccess(payrollService.findAll(filters)(NoExpansions())) { (records, count) =>
                completeAsPaginatedApiResponse(records, count)
              }
            }
        }
      }
    }
  }
}
