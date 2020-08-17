package io.paytouch.core.resources

import java.time.LocalDateTime
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.filters.{ ReportCustomerSummaryFilters, ReportProfitSummaryFilters, ReportSalesSummaryFilters }
import io.paytouch.core.services.ReportService

trait ReportResource extends JsonResource {

  def reportService: ReportService

  val reportRoutes: Route =
    path("reports.sales_summary") {
      get {
        parameters(
          "location_id".as[UUID].?,
          "from".as[LocalDateTime].?,
          "to".as[LocalDateTime].?,
        ) {
          case (locationId, from, to) =>
            authenticate { implicit user =>
              val filters = ReportSalesSummaryFilters(locationId, from, to)
              onSuccess(reportService.computeReportSalesSummary(filters)) { result =>
                completeAsApiResponse(result, "reports_sales_summary")
              }
            }
        }
      }
    } ~
      path("reports.profit_summary") {
        get {
          parameters("location_id".as[UUID].?, "from".as[LocalDateTime].?, "to".as[LocalDateTime].?) {
            case (locationId, from, to) =>
              authenticate { implicit user =>
                val filters = ReportProfitSummaryFilters(locationId, from, to)
                onSuccess(reportService.getProfitSummary(filters)) { result =>
                  completeAsApiResponse(result, "reports_profit_summary")
                }
              }
          }
        }
      } ~
      path("reports.customers_summary") {
        get {
          parameters("location_id".as[UUID].?, "from".as[LocalDateTime].?, "to".as[LocalDateTime].?) {
            case (locationId, from, to) =>
              authenticate { implicit user =>
                val filters = ReportCustomerSummaryFilters(locationId, from, to)
                onSuccess(reportService.getCustomerSummary(filters)) { result =>
                  completeAsApiResponse(result, "reports_customers_summary")
                }
              }
          }
        }
      }
}
