package io.paytouch.core.reports.entities

import java.util.UUID

import io.paytouch.core.data.model.CategoryRecord

final case class CategorySales(
    id: UUID,
    name: String,
    data: OrderItemSalesAggregate,
  )

object CategorySales {
  def apply(article: CategoryRecord, data: OrderItemSalesAggregate): CategorySales =
    CategorySales(article.id, article.name, data = data)
}
