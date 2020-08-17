package io.paytouch.core.reports.resources

import java.time.{ LocalDate, LocalDateTime }
import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.data.model.enums.OrderType
import io.paytouch.core.reports.entities.enums.CsvExports
import io.paytouch.core.reports.queries.CsvExport
import io.paytouch.core.reports.services.ExportService
import io.paytouch.core.reports.views._

trait ExportResource extends ReportResource {

  def exportService: ExportService

  val exportRoutes: Route =
    path("exports.get") {
      parameter("export_id".as[UUID]) { id =>
        get {
          authenticate { implicit user =>
            onSuccess(exportService.findById(id))(result => completeAsOptApiResponse(result))
          }
        }
      }
    } ~
      path("exports.download") {
        parameter("export_id".as[UUID]) { id =>
          get {
            authenticate { implicit user =>
              onSuccess(exportService.generatePresignedUrl(id))(result => completeAsValidatedApiResponse(result))
            }
          }
        }
      } ~ pathPrefix("exports") {
      exportAggrRoutes ~ exportTopRoutes ~ exportListRoutes ~ exportCsvRoutes
    }

  private val exportAggrRoutes = buildAggrRoutes(CustomerView) ~
    buildAggrRoutes(GiftCardPassView) ~
    buildAggrRoutes(LocationGiftCardPassView()) ~
    buildAggrRoutes(LocationSalesView()) ~
    buildAggrRoutes(OrderView) ~
    buildAggrRoutes(OrderTaxRateView) ~
    buildAggrRoutes(ProductSalesView(variantService)) ~
    buildAggrRoutes(SalesView())

  private def buildAggrRoutes(
      view: ReportAggrView,
    )(implicit
      umGroupBy: EnumUnmarshaller[view.GroupBy],
      umFields: SeqEnumUnmarshaller[view.Field],
    ) =
    countViewEndpoint(post, view) { implicit user => filters =>
      parameters("filename") { filename =>
        onSuccess(exportService.count(filename, filters))(result => completeAsApiResponse(result))
      }

    } ~ sumViewEndpoint(post, view) { implicit user => filters =>
      parameters("filename") { filename =>
        onSuccess(exportService.sum(filename, filters))(result => completeAsApiResponse(result))
      }
    } ~ averageViewEndpoint(post, view) { implicit user => filters =>
      parameters("filename") { filename =>
        onSuccess(exportService.average(filename, filters))(result => completeAsApiResponse(result))
      }
    }

  private val exportTopRoutes = buildTopRoutes(CustomerView) ~
    buildTopRoutes(GroupView) ~
    buildTopRoutes(ProductView(variantService))

  private def buildTopRoutes(view: ReportTopView)(implicit umCriterion: SeqEnumUnmarshaller[view.OrderBy]) =
    topViewEndpoint(post, view) { implicit user => filters =>
      parameters("filename") { filename =>
        onSuccess(exportService.top(filename, filters))(result => completeAsApiResponse(result))
      }
    }

  private val exportListRoutes = buildListRoutes(CategorySalesView) ~
    buildListRoutes(EmployeeSalesView()) ~
    buildListRoutes(LocationGiftCardPassView()) ~
    buildListRoutes(LocationSalesView()) ~
    buildListRoutes(ProductSalesView(variantService))

  private def buildListRoutes(
      view: ReportListView,
    )(implicit
      umCriterion: SeqEnumUnmarshaller[view.OrderBy],
      umOrderType: SeqEnumUnmarshaller[OrderType],
    ) =
    listViewEndpoint(post, view) { implicit context => implicit user => filters =>
      parameters("filename") { filename =>
        onSuccess(exportService.list(filename, filters))(result => completeAsApiResponse(result))
      }
    }

  private val exportCsvRoutes =
    concat(
      path(s"customers.single") {
        post {
          parameters("filename") { filename =>
            authenticate { implicit user =>
              onSuccess(exportService.single(CsvExports.Customers, filename))(result => completeAsApiResponse(result))
            }
          }
        }
      },
      path(s"inventory_count.single") {
        post {
          parameters("filename") { filename =>
            authenticate { implicit user =>
              onSuccess(exportService.single(CsvExports.InventoryCount, filename)) { result =>
                completeAsApiResponse(result)
              }
            }
          }
        }
      },
      path(s"reimportable_products.single") {
        post {
          parameters("filename") { filename =>
            authenticate { implicit user =>
              onSuccess(exportService.single(CsvExports.ReimportableProducts, filename)) { result =>
                completeAsApiResponse(result)
              }
            }
          }
        }
      },
      path(s"cash_drawers.single") {
        post {
          parameters("filename") { filename =>
            parameters("from".as[LocalDateTime], "to".as[LocalDateTime]) {
              case (from, to) =>
                authenticate { implicit user =>
                  implicit val exportQuery = CsvExport.cashDrawersQuery(from, to)
                  onSuccess(exportService.single(CsvExports.CashDrawers, filename)) { result =>
                    completeAsApiResponse(result)
                  }
                }
            }
          }
        }
      },
    )
}
