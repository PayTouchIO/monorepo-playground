package io.paytouch.core.reports.resources.ordertaxrates

import io.paytouch.core.reports.resources.ReportsAggrFSpec
import io.paytouch.core.reports.views.OrderTaxRateView
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

abstract class OrderTaxRatesFSpec extends ReportsAggrFSpec[OrderTaxRateView] {

  def view = OrderTaxRateView

  val fixtures = new OrderTaxRatesFSpecContext

  class OrderTaxRatesFSpecContext extends ReportsAggrFSpecContext with OrderTaxRatesFSpecFixtures
}
