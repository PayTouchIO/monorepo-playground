package io.paytouch.ordering.conversions

import java.util.UUID

import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.ModifierSetType
import io.paytouch.ordering.clients.paytouch.core.entities.Product

trait CartItemBundleSetConversions {

  protected def toCartItemBundleSets(
      validBundleSets: Seq[ValidCartItemBundleSetUpsertion],
    )(implicit
      store: StoreContext,
    ): Seq[CartItemBundleSet] =
    validBundleSets.map(toCartItemBundleSet)

  protected def toCartItemBundleSet(
      validBundleSet: ValidCartItemBundleSetUpsertion,
    )(implicit
      store: StoreContext,
    ): CartItemBundleSet =
    CartItemBundleSet(
      bundleSetId = validBundleSet.upsertion.bundleSetId,
      name = validBundleSet.coreData.name,
      position = validBundleSet.position,
      cartItemBundleOptions = validBundleSet.bundleOptions.map(toCartItemBundleOption),
    )

  protected def toCartItemBundleOption(
      validBundleOption: ValidCartItemBundleOptionUpsertion,
    )(implicit
      store: StoreContext,
    ): CartItemBundleOption =
    CartItemBundleOption(
      bundleOptionId = validBundleOption.upsertion.bundleOptionId,
      item = toCartItemBundleOptionItem(validBundleOption),
      priceAdjustment = validBundleOption.coreData._2.priceAdjustment,
      position = validBundleOption.position,
    )

  protected def toCartItemBundleOptionItem(
      validBundleOption: ValidCartItemBundleOptionUpsertion,
    )(implicit
      store: StoreContext,
    ): CartItemBundleOptionItem =
    CartItemBundleOptionItem(
      product = toCartItemProduct(validBundleOption.coreData._3),
      quantity = validBundleOption.upsertion.quantity,
      unit = validBundleOption.coreData._3.unit,
      cost = validBundleOption.coreData._3.locationOverrides.get(store.locationId).flatMap(_.cost),
      notes = validBundleOption.upsertion.notes,
      modifierOptions = validBundleOption.modifierOptions.map(toCartItemModifierOption),
      variantOptions = validBundleOption.variantOptions.map(toCartItemVariantOption),
    )

  protected def toCartItemModifierOption(
      validModifierOption: ValidCartItemModifierOptionUpsertion,
    )(implicit
      store: StoreContext,
    ): CartItemModifierOption =
    CartItemModifierOption(
      // unused value at this level, setting to random vs creating another class without the `id` field
      id = UUID.randomUUID,
      modifierOptionId = validModifierOption.upsertion.modifierOptionId,
      name = validModifierOption.coreData._2.name,
      `type` = validModifierOption.coreData._1.`type`,
      price = validModifierOption.coreData._2.price,
      quantity = validModifierOption.upsertion.quantity.getOrElse(1),
    )

  protected def toCartItemVariantOption(
      validVariantOption: ValidCartItemVariantOptionUpsertion,
    )(implicit
      store: StoreContext,
    ): CartItemVariantOption =
    CartItemVariantOption(
      // unused value at this level, setting to random vs creating another class without the `id` field
      id = UUID.randomUUID,
      variantOptionId = validVariantOption.coreData.id,
      optionName = validVariantOption.coreData.name,
      optionTypeName = validVariantOption.coreData.typeName,
    )

  private def toCartItemProduct(product: Product): CartItemProduct =
    CartItemProduct(id = product.id, name = product.name, description = product.description)

}
