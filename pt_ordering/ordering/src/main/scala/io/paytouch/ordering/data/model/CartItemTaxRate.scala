package io.paytouch.ordering.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.entities.Default

final case class CartItemTaxRateRecord(
    id: UUID,
    storeId: UUID,
    cartItemId: UUID,
    taxRateId: UUID,
    name: String,
    value: BigDecimal,
    totalAmount: BigDecimal,
    applyToPrice: Boolean,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickStoreRecord

final case class CartItemTaxRateUpdate(
    id: Default[UUID],
    storeId: Option[UUID],
    cartItemId: Option[UUID],
    taxRateId: Option[UUID],
    name: Option[String],
    value: Option[BigDecimal],
    totalAmount: Option[BigDecimal],
    applyToPrice: Option[Boolean],
  ) extends SlickUpdate[CartItemTaxRateRecord] {

  def toRecord: CartItemTaxRateRecord = {
    requires(
      "store id" -> storeId,
      "cart item id" -> cartItemId,
      "tax rate id" -> taxRateId,
      "name" -> name,
      "value" -> value,
      "total amount" -> totalAmount,
      "apply to price" -> applyToPrice,
    )
    CartItemTaxRateRecord(
      id = id.getOrDefault,
      storeId = storeId.get,
      cartItemId = cartItemId.get,
      taxRateId = taxRateId.get,
      name = name.get,
      value = value.get,
      totalAmount = totalAmount.get,
      applyToPrice = applyToPrice.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CartItemTaxRateRecord): CartItemTaxRateRecord =
    CartItemTaxRateRecord(
      id = id.getOrElse(record.id),
      storeId = storeId.getOrElse(record.storeId),
      cartItemId = cartItemId.getOrElse(record.cartItemId),
      taxRateId = taxRateId.getOrElse(record.taxRateId),
      name = name.getOrElse(record.name),
      value = value.getOrElse(record.value),
      totalAmount = totalAmount.getOrElse(record.totalAmount),
      applyToPrice = applyToPrice.getOrElse(record.applyToPrice),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object CartItemTaxRateUpdate {

  val empty: CartItemTaxRateUpdate =
    apply(
      id = None,
      storeId = None,
      cartItemId = None,
      taxRateId = None,
      name = None,
      value = None,
      totalAmount = None,
      applyToPrice = None,
    )
}
