package io.paytouch.core.reports.resources.categorysales

import io.paytouch.core.reports.resources.ReportsListFSpec
import io.paytouch.core.reports.resources.orders.OrdersFSpecFixtures
import io.paytouch.core.reports.views.CategorySalesView

abstract class CategorySalesFSpec extends ReportsListFSpec[CategorySalesView] {

  def view = CategorySalesView

  val fixtures = new CategorySalesFSpecContext

  class CategorySalesFSpecContext extends ReportsListFSpecContext with OrdersFSpecFixtures
}
