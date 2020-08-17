package io.paytouch.ordering.conversions

import java.util.UUID

import cats.implicits._

import io.paytouch.ordering.data.model.{ CartItemRecord, CartItemUpdate }
import io.paytouch.ordering.data.model.upsertions.{ CartItemUpsertion => CartItemUpsertionModel }
import io.paytouch.ordering.entities.{ CartItem => CartItemEntity, _ }

trait CartItemConversions
    extends CartItemModifierOptionConversions
       with CartItemVariantOptionConversions
       with CartItemTaxRateConversions
       with CartItemBundleSetConversions {
  protected def fromRecordToEntity(record: CartItemRecord)(implicit context: AppContext): CartItemEntity =
    CartItemEntity(
      id = record.id,
      product = toCartItemProduct(record),
      quantity = record.quantity,
      unit = record.unit,
      price = MonetaryAmount(record.priceAmount),
      cost = MonetaryAmount.extract(record.costAmount),
      tax = MonetaryAmount(record.taxAmount),
      calculatedPrice = MonetaryAmount(record.calculatedPriceAmount),
      totalPrice = MonetaryAmount(record.totalPriceAmount),
      notes = record.notes,
      modifierOptions = Seq.empty,
      taxRates = Seq.empty,
      variantOptions = Seq.empty,
      bundleSets = record.bundleSets,
      `type` = record.`type`,
      giftCardData = record.giftCardData,
    )

  private def toCartItemProduct(record: CartItemRecord): CartItemProduct =
    CartItemProduct(
      id = record.productId,
      name = record.productName,
      description = record.productDescription,
    )

  protected def toUpsertionModel(
      id: UUID,
      upsertion: ValidCartItemUpsertion,
    )(implicit
      store: StoreContext,
    ): CartItemUpsertionModel =
    CartItemUpsertionModel(
      cartItem = toItemUpdateModel(id, upsertion),
      cartItemModifierOptions = toItemModifierOptionUpdateModels(id, upsertion),
      cartItemTaxRates = toItemTaxRateUpdateModels(id, upsertion),
      cartItemVariantOptions = toItemVariantOptionUpdateModels(id, upsertion),
    )

  private def toItemUpdateModel(id: UUID, upsertion: ValidCartItemUpsertion)(implicit store: StoreContext) =
    CartItemUpdate(
      id = Some(id),
      storeId = Some(store.id),
      cartId = upsertion.upsertion.cartId,
      productId = upsertion.upsertion.productId,
      productName = upsertion.coreData.map(_.name),
      productDescription = upsertion.coreData.map(_.description),
      quantity = upsertion.upsertion.quantity,
      unit = upsertion.coreData.map(_.unit),
      priceAmount =
        if (upsertion.upsertion.isGiftCard)
          upsertion
            .upsertion
            .giftCardData
            .map(_.amount)
        else
          upsertion
            .coreData
            .flatMap(_.locationOverrides.get(store.locationId).map(_.price.amount)),
      costAmount = upsertion.coreData.flatMap(_.locationOverrides.get(store.locationId).flatMap(_.cost.map(_.amount))),
      taxAmount = Some(0),
      calculatedPriceAmount = Some(0),
      totalPriceAmount = Some(0),
      notes = upsertion.upsertion.notes,
      bundleSets = upsertion.bundleSets.map(toCartItemBundleSets),
      `type` = upsertion.upsertion.`type`,
      giftCardData = upsertion.upsertion.giftCardData,
    )

  protected def findMergeable(cart: Cart, creation: CartItemCreation): Option[CartItemEntity] =
    cart.items.find { item =>
      lazy val doesProductIdMatch =
        item.product.id === creation.productId

      lazy val doModifierOptionIdsMatch = {
        val itemModifierOptionIds =
          item
            .modifierOptions
            .map(_.modifierOptionId)
            .toSet

        val creationModifierOptionIds =
          creation
            .modifierOptions
            .map(_.modifierOptionId)
            .toSet

        itemModifierOptionIds === creationModifierOptionIds
      }

      lazy val doBundleOptionIdsMatch = {
        val itemBundleOptionIds =
          item
            .bundleSets
            .getOrElse(Seq.empty)
            .flatMap(_.cartItemBundleOptions.map(_.bundleOptionId))
            .toSet

        val creationBundleOptionIds =
          creation
            .bundleSets
            .getOrElse(Seq.empty)
            .flatMap(_.bundleOptions.map(_.bundleOptionId))
            .toSet

        itemBundleOptionIds === creationBundleOptionIds
      }

      lazy val isNotGiftCard: Boolean =
        !creation.isGiftCard

      doesProductIdMatch &&
      doModifierOptionIdsMatch &&
      doBundleOptionIdsMatch &&
      isNotGiftCard
    }
}
