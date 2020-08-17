package io.paytouch.ordering.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.{ BundleOption, BundleSet, Product }
import io.paytouch.ordering.entities.enums.UnitType

final case class CartItemBundleSetCreation(bundleSetId: UUID, bundleOptions: Seq[CartItemBundleOptionCreation]) {
  def asUpsert: CartItemBundleSetUpsertion =
    CartItemBundleSetUpsertion(
      bundleSetId = bundleSetId,
      bundleOptions = bundleOptions.map(_.asUpsert),
    )
}

final case class CartItemBundleOptionCreation(
    bundleOptionId: UUID,
    quantity: BigDecimal,
    notes: Option[String],
    modifierOptions: Seq[CartItemModifierOptionCreation],
  ) {
  def asUpsert: CartItemBundleOptionUpsertion =
    CartItemBundleOptionUpsertion(
      bundleOptionId = bundleOptionId,
      quantity = quantity,
      notes = notes,
      modifierOptions = modifierOptions.map(_.asUpsert),
    )
}

final case class CartItemBundleSetUpsertion(bundleSetId: UUID, bundleOptions: Seq[CartItemBundleOptionUpsertion])

final case class CartItemBundleOptionUpsertion(
    bundleOptionId: UUID,
    quantity: BigDecimal,
    notes: Option[String],
    modifierOptions: Seq[CartItemModifierOptionUpsertion],
  )

final case class ValidCartItemBundleSetUpsertion(
    upsertion: CartItemBundleSetUpsertion,
    coreData: BundleSet,
    position: Int,
    bundleOptions: Seq[ValidCartItemBundleOptionUpsertion],
  )

final case class ValidCartItemBundleOptionUpsertion(
    upsertion: CartItemBundleOptionUpsertion,
    coreData: (BundleSet, BundleOption, Product),
    position: Int,
    modifierOptions: Seq[ValidCartItemModifierOptionUpsertion],
    variantOptions: Seq[ValidCartItemVariantOptionUpsertion],
  )

final case class CartItemBundleSet(
    bundleSetId: UUID,
    name: Option[String],
    position: Int,
    cartItemBundleOptions: Seq[CartItemBundleOption],
  )

final case class CartItemBundleOption(
    bundleOptionId: UUID,
    item: CartItemBundleOptionItem,
    priceAdjustment: BigDecimal,
    position: Int,
  )

final case class CartItemBundleOptionItem(
    product: CartItemProduct,
    quantity: BigDecimal,
    unit: UnitType,
    cost: Option[MonetaryAmount],
    notes: Option[String],
    modifierOptions: Seq[CartItemModifierOption],
    variantOptions: Seq[CartItemVariantOption],
  )
