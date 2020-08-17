package io.paytouch.core.reports.resources

import io.paytouch.core.data.model.enums.OrderType
import io.paytouch.core.reports.services.EngineService
import io.paytouch.core.reports.views._

trait EngineResource extends ReportResource {

  def engineService: EngineService

  val engineRoutes = pathPrefix("reports") {
    engineAggrRoutes ~ engineTopRoutes ~ engineListRoutes
  }

  private val engineAggrRoutes =
    buildAggrRoutes(CustomerView) ~
      buildAggrRoutes(GiftCardPassView) ~
      buildAggrRoutes(LocationGiftCardPassView()) ~
      buildAggrRoutes(LocationSalesView()) ~
      buildAggrRoutes(OrderTaxRateView) ~
      buildAggrRoutes(OrderView) ~
      buildAggrRoutes(ProductSalesView(variantService)) ~
      buildAggrRoutes(SalesView()) ~
      buildAggrRoutes(LoyaltyOrdersView) ~
      buildAggrRoutes(RewardRedemptionsView)

  private def buildAggrRoutes(
      view: ReportAggrView,
    )(implicit
      umGroupBy: EnumUnmarshaller[view.GroupBy],
      umFields: SeqEnumUnmarshaller[view.Field],
    ) =
    countViewEndpoint(get, view) { implicit user => filters =>
      onSuccess(engineService.count(filters))(result => completeAsEngineResponse(result))
    } ~ sumViewEndpoint(get, view) { implicit user => filters =>
      onSuccess(engineService.sum(filters))(result => completeAsEngineResponse(result))
    } ~ averageViewEndpoint(get, view) { implicit user => filters =>
      onSuccess(engineService.average(filters))(result => completeAsEngineResponse(result))
    }

  private val engineTopRoutes = buildTopRoutes(CustomerView) ~
    buildTopRoutes(ProductView(variantService)) ~
    buildTopRoutes(GroupView)

  private def buildTopRoutes(view: ReportTopView)(implicit umCriterion: SeqEnumUnmarshaller[view.OrderBy]) =
    topViewEndpoint(get, view) { implicit user => filters =>
      onSuccess(engineService.top(filters))(result => completeAsEngineResponse(result))
    }

  private val engineListRoutes = buildListRoutes(CategorySalesView) ~
    buildListRoutes(LocationGiftCardPassView()) ~
    buildListRoutes(LocationSalesView()) ~
    buildListRoutes(EmployeeSalesView()) ~
    buildListRoutes(ProductSalesView(variantService))

  private def buildListRoutes(
      view: ReportListView,
    )(implicit
      umCriterion: SeqEnumUnmarshaller[view.OrderBy],
      umOrderType: SeqEnumUnmarshaller[OrderType],
    ) =
    listViewEndpoint(get, view) { implicit context => implicit user => filters =>
      onSuccess(engineService.list(filters))(result => completeAsEngineResponse(result))
    }

}
