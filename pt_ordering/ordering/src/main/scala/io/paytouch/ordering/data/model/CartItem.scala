package io.paytouch.ordering.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CartItemType
import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.UnitType

final case class CartItemRecord(
    id: UUID,
    storeId: UUID,
    cartId: UUID,
    productId: UUID,
    productName: String,
    productDescription: Option[String],
    quantity: BigDecimal,
    unit: UnitType,
    priceAmount: BigDecimal,
    costAmount: Option[BigDecimal],
    taxAmount: BigDecimal,
    calculatedPriceAmount: BigDecimal,
    totalPriceAmount: BigDecimal,
    notes: Option[String],
    bundleSets: Option[Seq[CartItemBundleSet]],
    `type`: CartItemType,
    giftCardData: Option[GiftCardData],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickStoreRecord {
  val isGiftCard: Boolean =
    `type` == CartItemType.GiftCard && giftCardData.isDefined
}

case class CartItemUpdate(
    id: Default[UUID],
    storeId: Option[UUID],
    cartId: Option[UUID],
    productId: Option[UUID],
    productName: Option[String],
    productDescription: ResettableString,
    quantity: Option[BigDecimal],
    unit: Option[UnitType],
    priceAmount: Option[BigDecimal],
    costAmount: ResettableBigDecimal,
    taxAmount: Option[BigDecimal],
    calculatedPriceAmount: Option[BigDecimal],
    totalPriceAmount: Option[BigDecimal],
    notes: ResettableString,
    bundleSets: Option[Seq[CartItemBundleSet]],
    `type`: Option[CartItemType],
    giftCardData: Option[GiftCardData],
  ) extends SlickUpdate[CartItemRecord] {
  def toRecord: CartItemRecord = {
    requires(
      "store id" -> storeId,
      "cart id" -> cartId,
      "product id" -> productId,
      "product name" -> productName,
      "quantity" -> quantity,
      "unit" -> unit,
      "price amount" -> priceAmount,
      "tax amount" -> taxAmount,
      "calculated price amount" -> calculatedPriceAmount,
      "total price amount" -> totalPriceAmount,
      "type" -> `type`,
    )

    CartItemRecord(
      id = id.getOrDefault,
      storeId = storeId.get,
      cartId = cartId.get,
      productId = productId.get,
      productName = productName.get,
      productDescription = productDescription,
      quantity = quantity.get,
      unit = unit.get,
      priceAmount = priceAmount.get,
      costAmount = costAmount,
      taxAmount = taxAmount.get,
      calculatedPriceAmount = calculatedPriceAmount.get,
      totalPriceAmount = totalPriceAmount.get,
      notes = notes,
      bundleSets = bundleSets,
      `type` = `type`.get,
      giftCardData = giftCardData,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: CartItemRecord): CartItemRecord =
    CartItemRecord(
      id = id.getOrElse(record.id),
      storeId = storeId.getOrElse(record.storeId),
      cartId = cartId.getOrElse(record.cartId),
      productId = productId.getOrElse(record.productId),
      productName = productName.getOrElse(record.productName),
      productDescription = productDescription.getOrElse(record.productDescription),
      quantity = quantity.getOrElse(record.quantity),
      unit = unit.getOrElse(record.unit),
      priceAmount = priceAmount.getOrElse(record.priceAmount),
      costAmount = costAmount.getOrElse(record.costAmount),
      taxAmount = taxAmount.getOrElse(record.taxAmount),
      calculatedPriceAmount = calculatedPriceAmount.getOrElse(record.calculatedPriceAmount),
      totalPriceAmount = totalPriceAmount.getOrElse(record.totalPriceAmount),
      notes = notes.getOrElse(record.notes),
      bundleSets = bundleSets.orElse(record.bundleSets),
      `type` = record.`type`, // set only on creation
      giftCardData = giftCardData.orElse(record.giftCardData),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object CartItemUpdate {
  val empty: CartItemUpdate =
    CartItemUpdate(
      id = None,
      storeId = None,
      cartId = None,
      productId = None,
      productName = None,
      productDescription = None,
      quantity = None,
      unit = None,
      priceAmount = None,
      costAmount = None,
      taxAmount = None,
      calculatedPriceAmount = None,
      totalPriceAmount = None,
      notes = None,
      bundleSets = None,
      `type` = None,
      giftCardData = None,
    )
}
