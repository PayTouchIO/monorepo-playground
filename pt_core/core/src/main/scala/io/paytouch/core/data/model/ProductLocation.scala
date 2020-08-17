package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.core.data.model.enums.UnitType

final case class ProductLocationRecord(
    id: UUID,
    merchantId: UUID,
    productId: UUID,
    locationId: UUID,
    priceAmount: BigDecimal,
    costAmount: Option[BigDecimal],
    averageCostAmount: Option[BigDecimal],
    unit: UnitType,
    margin: Option[BigDecimal],
    active: Boolean,
    routeToKitchenId: Option[UUID],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickProductRecord
       with SlickToggleableRecord
       with SlickItemLocationRecord {
  def itemId = productId

  def contains(pId: UUID, lId: UUID): Boolean = productId == pId && locationId == lId
}

case class ProductLocationUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    productId: Option[UUID],
    locationId: Option[UUID],
    priceAmount: Option[BigDecimal],
    costAmount: Option[BigDecimal],
    averageCostAmount: Option[BigDecimal],
    unit: Option[UnitType],
    margin: Option[BigDecimal],
    active: Option[Boolean],
    routeToKitchenId: Option[UUID],
  ) extends SlickProductUpdate[ProductLocationRecord] {

  def toRecord: ProductLocationRecord = {
    require(merchantId.isDefined, s"Impossible to convert ProductLocationUpdate without a merchant id. [$this]")
    require(productId.isDefined, s"Impossible to convert ProductLocationUpdate without a product id. [$this]")
    require(locationId.isDefined, s"Impossible to convert ProductLocationUpdate without a location id. [$this]")
    require(priceAmount.isDefined, s"Impossible to convert ProductLocationUpdate without a price amount. [$this]")
    require(unit.isDefined, s"Impossible to convert ProductLocationUpdate without a unit. [$this]")
    ProductLocationRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      productId = productId.get,
      locationId = locationId.get,
      priceAmount = priceAmount.get,
      costAmount = costAmount,
      averageCostAmount = averageCostAmount,
      unit = unit.get,
      margin = margin,
      active = active.getOrElse(true),
      routeToKitchenId = routeToKitchenId,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ProductLocationRecord): ProductLocationRecord =
    ProductLocationRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      productId = productId.getOrElse(record.productId),
      locationId = locationId.getOrElse(record.locationId),
      priceAmount = priceAmount.getOrElse(record.priceAmount),
      costAmount = costAmount.orElse(record.costAmount),
      averageCostAmount = averageCostAmount.orElse(record.averageCostAmount),
      unit = unit.getOrElse(record.unit),
      margin = margin.orElse(record.margin),
      active = active.getOrElse(record.active),
      routeToKitchenId = routeToKitchenId.orElse(record.routeToKitchenId),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
