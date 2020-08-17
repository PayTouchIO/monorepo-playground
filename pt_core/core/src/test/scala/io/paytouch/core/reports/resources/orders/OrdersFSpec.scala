package io.paytouch.core.reports.resources.orders

import io.paytouch.core.reports.resources.ReportsAggrFSpec
import io.paytouch.core.reports.views.OrderView

abstract class OrdersFSpec extends ReportsAggrFSpec[OrderView] {

  def view = OrderView

  val fixtures = new OrdersFSpecContext

  class OrdersFSpecContext extends ReportsAggrFSpecContext with OrdersFSpecFixtures
}
