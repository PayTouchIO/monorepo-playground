package io.paytouch.core.reports.resources.locationsales

import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.entities.{ LocationSales, ReportFields, SalesAggregate }
import io.paytouch.core.reports.resources.ReportsAggrFSpec
import io.paytouch.core.reports.views.LocationSalesView

abstract class LocationSalesFSpec extends ReportsAggrFSpec[LocationSalesView] {
  def view = LocationSalesView()

  val fixtures = new SalesFSpecContext

  @scala.annotation.nowarn("msg=Auto-application")
  implicit val mockUserContext =
    random[UserContext].copy(currency = fixtures.currency)

  class SalesFSpecContext extends ReportsAggrFSpecContext with LocationSalesFSpecFixtures {

    def londonResult(data: SalesAggregate) =
      ReportFields(
        key = Some(london.id.toString),
        LocationSales(id = london.id, name = london.name, addressLine1 = london.addressLine1, data = data),
      )

    def romeResult(data: SalesAggregate) =
      ReportFields(
        key = Some(rome.id.toString),
        LocationSales(id = rome.id, name = rome.name, addressLine1 = rome.addressLine1, data = data),
      )

    def orderedResult(londonData: SalesAggregate, romeData: SalesAggregate) =
      Seq(londonResult(londonData), romeResult(romeData))

  }
}
