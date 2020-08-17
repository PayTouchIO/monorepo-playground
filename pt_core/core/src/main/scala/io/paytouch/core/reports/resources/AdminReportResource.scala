package io.paytouch.core.reports.resources

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.reports.filters.AdminReportFilters
import io.paytouch.core.reports.services.AdminReportService
import io.paytouch.core.resources.{ AdminAuthentication, JsonResource }

trait AdminReportResource extends JsonResource with AdminAuthentication {

  def adminReportService: AdminReportService

  val adminReportRoutes: Route =
    path("reports.generate") {
      post {
        parameters(
          "id[]".as[Seq[UUID]].?,
          "merchant_id[]".as[Seq[UUID]].?,
          "location_id[]".as[Seq[UUID]].?,
          "from".as[ZonedDateTime].?,
          "to".as[ZonedDateTime].?,
        ).as(AdminReportFilters.apply _) { filters =>
          authenticateAdmin { implicit admin =>
            onSuccess(adminReportService.adminRecomputeReports(filters)) { count =>
              completeAsApiResponse(count, "triggered-reports-count")
            }
          }
        }
      }
    }

}
