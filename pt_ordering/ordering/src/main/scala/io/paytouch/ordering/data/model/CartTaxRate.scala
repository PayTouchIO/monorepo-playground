package io.paytouch.ordering.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.entities.Default

final case class CartTaxRateRecord(
    id: UUID,
    storeId: UUID,
    cartId: UUID,
    taxRateId: UUID,
    name: String,
    `value`: BigDecimal,
    totalAmount: BigDecimal,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickStoreRecord

final case class CartTaxRateUpdate(
    id: Default[UUID],
    storeId: Option[UUID],
    cartId: Option[UUID],
    taxRateId: Option[UUID],
    name: Option[String],
    `value`: Option[BigDecimal],
    totalAmount: Option[BigDecimal],
  ) extends SlickUpdate[CartTaxRateRecord] {

  def toRecord: CartTaxRateRecord = {
    requires(
      "store id" -> storeId,
      "cart id" -> cartId,
      "tax rate id" -> taxRateId,
      "name" -> name,
      "value" -> `value`,
      "total amount" -> totalAmount,
    )
    CartTaxRateRecord(
      id = id.getOrDefault,
      storeId = storeId.get,
      cartId = cartId.get,
      taxRateId = taxRateId.get,
      name = name.get,
      `value` = `value`.get,
      totalAmount = totalAmount.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CartTaxRateRecord): CartTaxRateRecord =
    CartTaxRateRecord(
      id = id.getOrElse(record.id),
      storeId = storeId.getOrElse(record.storeId),
      cartId = cartId.getOrElse(record.cartId),
      taxRateId = taxRateId.getOrElse(record.taxRateId),
      name = name.getOrElse(record.name),
      `value` = `value`.getOrElse(record.`value`),
      totalAmount = totalAmount.getOrElse(record.totalAmount),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object CartTaxRateUpdate {

  val empty: CartTaxRateUpdate = apply(None, None, None, None, None, None, None)
}
