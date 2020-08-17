package io.paytouch.ordering.entities

import java.util.UUID

import cats.implicits._

import io.scalaland.chimney.dsl._

import io.paytouch.implicits._

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CartItemType
import io.paytouch.ordering.clients.paytouch.core.entities.Product
import io.paytouch.ordering.entities.enums.UnitType

final case class CartItem(
    id: UUID,
    product: CartItemProduct,
    quantity: BigDecimal,
    unit: UnitType,
    price: MonetaryAmount,
    cost: Option[MonetaryAmount],
    tax: MonetaryAmount,
    calculatedPrice: MonetaryAmount,
    totalPrice: MonetaryAmount,
    notes: Option[String],
    modifierOptions: Seq[CartItemModifierOption],
    taxRates: Seq[CartItemTaxRate],
    variantOptions: Seq[CartItemVariantOption],
    bundleSets: Option[Seq[CartItemBundleSet]],
    `type`: CartItemType,
    giftCardData: Option[GiftCardData],
  ) {
  lazy val isCombo: Boolean =
    bundleSets
      .getOrElse(Seq.empty)
      .nonEmpty

  val isGiftCard: Boolean =
    `type` == CartItemType.GiftCard && giftCardData.isDefined
}

final case class CartItemCreation(
    cartId: UUID,
    productId: UUID,
    quantity: BigDecimal,
    notes: Option[String],
    modifierOptions: Seq[CartItemModifierOptionCreation],
    bundleSets: Option[Seq[CartItemBundleSetCreation]],
    `type`: CartItemType,
    giftCardData: Option[GiftCardData],
  ) extends CreationEntity[CartItemUpsertion] {
  def asUpsert: CartItemUpsertion =
    this
      .into[CartItemUpsertion]
      .withFieldConst(_.notes, notes: ResettableString)
      .transform

  val isGiftCard: Boolean =
    `type` == CartItemType.GiftCard && giftCardData.isDefined
}

final case class ProductCartItemCreation(
    cartId: UUID,
    productId: UUID,
    quantity: BigDecimal,
    notes: Option[String],
    modifierOptions: Seq[CartItemModifierOptionCreation],
    bundleSets: Option[Seq[CartItemBundleSetCreation]],
  ) extends CreationEntity[CartItemUpsertion] {
  def asUpsert: CartItemUpsertion =
    this
      .into[CartItemUpsertion]
      .enableOptionDefaultsToNone
      .withFieldConst(_.notes, notes: ResettableString)
      .withFieldConst(_.`type`, CartItemType.Product.some)
      .transform

  def toCartItemCreation: CartItemCreation =
    this
      .into[CartItemCreation]
      .enableOptionDefaultsToNone
      .withFieldConst(_.`type`, CartItemType.Product)
      .transform
}

final case class GiftCardCartItemCreation(
    cartId: UUID,
    productId: UUID,
    giftCardData: GiftCardData,
  ) extends CreationEntity[CartItemUpsertion] {
  def asUpsert: CartItemUpsertion =
    this
      .into[CartItemUpsertion]
      .enableOptionDefaultsToNone
      .withFieldConst(_.notes, None: ResettableString)
      .withFieldConst(_.quantity, 1.somew[BigDecimal])
      .withFieldConst(_.`type`, CartItemType.GiftCard.some)
      .transform

  def toCartItemCreation: CartItemCreation =
    this
      .into[CartItemCreation]
      .enableOptionDefaultsToNone
      .withFieldConst(_.quantity, 1: BigDecimal)
      .withFieldConst(_.modifierOptions, Seq.empty)
      .withFieldConst(_.`type`, CartItemType.GiftCard)
      .transform
}

final case class GiftCardData(recipientEmail: String, amount: BigDecimal)

final case class CartItemUpdate(
    productId: Option[UUID],
    quantity: Option[BigDecimal],
    notes: ResettableString,
    modifierOptions: Option[Seq[CartItemModifierOptionCreation]],
    bundleSets: Option[Seq[CartItemBundleSetCreation]],
    giftCardData: Option[GiftCardData],
  ) extends UpdateEntity[CartItemUpsertion] {
  def asUpsert: CartItemUpsertion =
    this
      .into[CartItemUpsertion]
      .enableOptionDefaultsToNone
      .withFieldConst(_.notes, notes: ResettableString)
      .transform
}

final case class ProductCartItemUpdate(
    productId: Option[UUID],
    quantity: Option[BigDecimal],
    notes: ResettableString,
    modifierOptions: Option[Seq[CartItemModifierOptionCreation]],
    bundleSets: Option[Seq[CartItemBundleSetCreation]],
  ) extends UpdateEntity[CartItemUpsertion] {
  def asUpsert: CartItemUpsertion =
    this
      .into[CartItemUpsertion]
      .enableOptionDefaultsToNone
      .withFieldConst(_.notes, None: ResettableString)
      .transform

  def toCartItemUpdate: CartItemUpdate =
    this
      .into[CartItemUpdate]
      .enableOptionDefaultsToNone
      .transform
}

final case class GiftCardCartItemUpdate(giftCardData: GiftCardData) extends UpdateEntity[CartItemUpsertion] {
  def asUpsert: CartItemUpsertion =
    this
      .into[CartItemUpsertion]
      .enableOptionDefaultsToNone
      .withFieldConst(_.notes, None: ResettableString)
      .transform

  def toCartItemUpdate: CartItemUpdate =
    this
      .into[CartItemUpdate]
      .enableOptionDefaultsToNone
      .withFieldConst(_.notes, None: ResettableString)
      .transform
}

final case class CartItemUpsertion(
    cartId: Option[UUID],
    productId: Option[UUID],
    quantity: Option[BigDecimal],
    notes: ResettableString,
    modifierOptions: Option[Seq[CartItemModifierOptionUpsertion]],
    bundleSets: Option[Seq[CartItemBundleSetUpsertion]],
    `type`: Option[CartItemType],
    giftCardData: Option[GiftCardData],
  ) {
  val isGiftCard: Boolean =
    // forall returns true for an empty Option which is what we want here!
    // when coming from GiftCardCartItemCreation this will be set to Some(GiftCard) so we can check against it
    // when coming from GiftCardCartItemUpdate this will be set to None so we check only against GiftCardData
    `type`.forall(_ == CartItemType.GiftCard) && giftCardData.isDefined
}

final case class ValidCartItemUpsertion(
    upsertion: CartItemUpsertion,
    coreData: Option[Product],
    modifierOptions: Option[Seq[ValidCartItemModifierOptionUpsertion]],
    bundleSets: Option[Seq[ValidCartItemBundleSetUpsertion]],
  )

object ValidCartItemUpsertion {
  def apply(upsertion: CartItemUpsertion): ValidCartItemUpsertion =
    ValidCartItemUpsertion(
      upsertion = upsertion,
      coreData = None,
      modifierOptions = None,
      bundleSets = None,
    )
}
