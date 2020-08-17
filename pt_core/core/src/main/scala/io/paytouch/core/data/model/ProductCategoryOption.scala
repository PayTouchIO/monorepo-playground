package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class ProductCategoryOptionRecord(
    id: UUID,
    merchantId: UUID,
    productCategoryId: UUID,
    deliveryEnabled: Boolean,
    takeAwayEnabled: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class ProductCategoryOptionUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productCategoryId: Option[UUID],
    deliveryEnabled: Option[Boolean],
    takeAwayEnabled: Option[Boolean],
  ) extends SlickMerchantUpdate[ProductCategoryOptionRecord] {

  def updateRecord(record: ProductCategoryOptionRecord): ProductCategoryOptionRecord =
    ProductCategoryOptionRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productCategoryId = productCategoryId.getOrElse(record.productCategoryId),
      deliveryEnabled = deliveryEnabled.getOrElse(record.deliveryEnabled),
      takeAwayEnabled = takeAwayEnabled.getOrElse(record.takeAwayEnabled),
      createdAt = record.createdAt,
      updatedAt = now,
    )

  def toRecord: ProductCategoryOptionRecord = {
    require(merchantId.isDefined, s"Impossible to convert ProductCategoryOptionUpdate without a merchant id. [$this]")
    require(
      productCategoryId.isDefined,
      s"Impossible to convert ProductCategoryOptionUpdate without a productCategoryId value. [$this]",
    )
    require(
      deliveryEnabled.isDefined,
      s"Impossible to convert ProductCategoryOptionUpdate without a deliveryEnabled value. [$this]",
    )
    require(
      takeAwayEnabled.isDefined,
      s"Impossible to convert ProductCategoryOptionUpdate without a takeAwayEnabled value. [$this]",
    )
    ProductCategoryOptionRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productCategoryId = productCategoryId.get,
      deliveryEnabled = deliveryEnabled.get,
      takeAwayEnabled = takeAwayEnabled.get,
      createdAt = now,
      updatedAt = now,
    )
  }
}
