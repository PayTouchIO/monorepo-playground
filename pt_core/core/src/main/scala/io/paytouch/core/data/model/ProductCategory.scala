package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class ProductCategoryRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    categoryId: UUID,
    position: Int,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickProductRecord

case class ProductCategoryUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    categoryId: Option[UUID],
    position: Option[Int],
  ) extends SlickProductUpdate[ProductCategoryRecord] {

  def toRecord: ProductCategoryRecord = {
    require(merchantId.isDefined, s"Impossible to convert ProductCategoryUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert ProductCategoryUpdate without a product id. [$this]")
    require(categoryId.isDefined, s"Impossible to convert ProductCategoryUpdate without a category id. [$this]")
    ProductCategoryRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      categoryId = categoryId.get,
      position = position.getOrElse(0),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ProductCategoryRecord): ProductCategoryRecord =
    ProductCategoryRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      categoryId = categoryId.getOrElse(record.categoryId),
      position = position.getOrElse(record.position),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

final case class ProductCategoryOrdering(
    productId: UUID,
    categoryId: UUID,
    position: Int,
  )
