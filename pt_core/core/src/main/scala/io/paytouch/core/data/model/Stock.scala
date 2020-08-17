package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class StockRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    locationId: UUID,
    quantity: BigDecimal,
    minimumOnHand: BigDecimal,
    reorderAmount: BigDecimal,
    sellOutOfStock: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord
       with SlickOneToOneWithLocationRecord {
  def contains(pId: UUID, lId: UUID): Boolean =
    productId == pId && locationId == lId
}

case class StockUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    locationId: Option[UUID],
    quantity: Option[BigDecimal],
    minimumOnHand: Option[BigDecimal],
    reorderAmount: Option[BigDecimal],
    sellOutOfStock: Option[Boolean],
  ) extends SlickMerchantUpdate[StockRecord] {

  def toRecord: StockRecord = {
    require(merchantId.isDefined, s"Impossible to convert StockUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert StockUpdate without a product id. [$this]")
    require(locationId.isDefined, s"Impossible to convert StockUpdate without a location id. [$this]")
    require(quantity.isDefined, s"Impossible to convert StockUpdate without a quantity. [$this]")
    StockRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      locationId = locationId.get,
      quantity = quantity.get,
      minimumOnHand = minimumOnHand.getOrElse(0),
      reorderAmount = reorderAmount.getOrElse(0),
      sellOutOfStock = sellOutOfStock.getOrElse(false),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: StockRecord): StockRecord =
    StockRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      locationId = locationId.getOrElse(record.locationId),
      quantity = quantity.getOrElse(record.quantity),
      minimumOnHand = minimumOnHand.getOrElse(record.minimumOnHand),
      reorderAmount = reorderAmount.getOrElse(record.reorderAmount),
      sellOutOfStock = sellOutOfStock.getOrElse(record.sellOutOfStock),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
