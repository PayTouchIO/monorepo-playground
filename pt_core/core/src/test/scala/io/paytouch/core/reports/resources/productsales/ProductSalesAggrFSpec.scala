package io.paytouch.core.reports.resources.productsales

import io.paytouch.core.reports.entities.{ OrderItemSalesAggregate, ProductSales, ReportFields }
import io.paytouch.core.reports.resources.ReportsAggrFSpec
import io.paytouch.core.reports.views.ProductSalesView
import io.paytouch.core.utils.MockedRestApi

abstract class ProductSalesAggrFSpec extends ReportsAggrFSpec[ProductSalesView] {

  def view = ProductSalesView(MockedRestApi.variantService)

  val fixtures = new ProductSalesAggrFSpecContext

  class ProductSalesAggrFSpecContext extends ReportsAggrFSpecContext with ProductSalesFSpecFixtures {
    val ids = emptyResultOrdered123AB.take(2).map(_.id)

    val emptyResultFieldsOrdered123 =
      emptyResultOrdered123AB.filter(productSales => ids.contains(productSales.id)).map(wrapInReportFields)

    val resultFieldsOrdered123 =
      resultOrdered123AB.filter(productSales => ids.contains(productSales.id)).map(wrapInReportFields)

    def wrapInReportFields(p: ProductSales) = ReportFields(key = Some(p.id.toString), values = p)

    def combineEmptyWithFull(f: (OrderItemSalesAggregate, OrderItemSalesAggregate) => OrderItemSalesAggregate) =
      emptyResultFieldsOrdered123.zip(resultFieldsOrdered123).map {
        case (emptyFields, fullFields) =>
          val newValues = emptyFields.values.copy(data = f(emptyFields.values.data, fullFields.values.data))
          emptyFields.copy(values = newValues)
      }
  }
}
