package io.paytouch.core.reports.resources.sales

import io.paytouch.core.entities.UserContext
import io.paytouch.core.reports.resources.ReportsAggrFSpec
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures
import io.paytouch.core.reports.views.SalesView

abstract class SalesFSpec extends ReportsAggrFSpec[SalesView] {
  def view = SalesView()

  val fixtures = new SalesFSpecContext

  @scala.annotation.nowarn("msg=Auto-application")
  implicit val mockUserContext =
    random[UserContext].copy(currency = fixtures.currency)

  class SalesFSpecContext extends ReportsAggrFSpecContext with OrdersFSpecFixtures {
    val totalCount = 4
  }
}
