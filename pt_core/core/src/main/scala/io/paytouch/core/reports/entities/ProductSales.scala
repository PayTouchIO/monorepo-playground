package io.paytouch.core.reports.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.ArticleRecord
import io.paytouch.core.entities.VariantOptionWithType

final case class ProductSales(
    id: UUID,
    name: String,
    sku: Option[String],
    upc: Option[String],
    deletedAt: Option[ZonedDateTime],
    options: Option[Seq[VariantOptionWithType]],
    data: OrderItemSalesAggregate,
  )

object ProductSales {
  def apply(article: ArticleRecord, data: OrderItemSalesAggregate): ProductSales =
    ProductSales(article.id, article.name, article.sku, article.upc, article.deletedAt, options = None, data = data)

  def apply(
      article: ArticleRecord,
      options: Seq[VariantOptionWithType],
      data: OrderItemSalesAggregate,
    ): ProductSales =
    ProductSales(
      article.id,
      article.name,
      article.sku,
      article.upc,
      article.deletedAt,
      options = Some(options),
      data = data,
    )
}
