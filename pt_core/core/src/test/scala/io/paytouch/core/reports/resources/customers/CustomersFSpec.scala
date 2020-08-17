package io.paytouch.core.reports.resources.customers

import io.paytouch.core.reports.resources.ReportsAggrFSpec
import io.paytouch.core.reports.views.CustomerView
import io.paytouch.core.utils.{ FixtureDaoFactory => Factory }

abstract class CustomersFSpec extends ReportsAggrFSpec[CustomerView] {

  def view = CustomerView

  val fixtures = new CustomersFSpecContext

  class CustomersFSpecContext extends ReportsAggrFSpecContext with CustomerFSpecFixtures
}
