package io.paytouch.ordering.conversions

import java.util.UUID

import io.paytouch.ordering.data.model.{ CartItemModifierOptionRecord, CartItemModifierOptionUpdate }
import io.paytouch.ordering.entities.{
  AppContext,
  MonetaryAmount,
  StoreContext,
  ValidCartItemModifierOptionUpsertion,
  ValidCartItemUpsertion,
  CartItemModifierOption => CartItemModifierOptionEntity,
  CartItemModifierOptionUpsertion => CartItemModifierOptionUpsertionEntity,
}

trait CartItemModifierOptionConversions {

  private type Upsertion = CartItemModifierOptionUpsertionEntity

  protected def fromRecordToEntity(
      record: CartItemModifierOptionRecord,
    )(implicit
      context: AppContext,
    ): CartItemModifierOptionEntity =
    CartItemModifierOptionEntity(
      id = record.id,
      modifierOptionId = record.modifierOptionId,
      name = record.name,
      `type` = record.`type`,
      price = MonetaryAmount(record.priceAmount),
      quantity = record.quantity,
    )

  protected def toItemModifierOptionUpdateModels(
      itemId: UUID,
      upsertion: ValidCartItemUpsertion,
    )(implicit
      store: StoreContext,
    ): Option[Seq[CartItemModifierOptionUpdate]] =
    upsertion.modifierOptions.map(_.map(toItemModifierOptionUpdateModel(itemId, _)))

  private def toItemModifierOptionUpdateModel(
      itemId: UUID,
      upsertion: ValidCartItemModifierOptionUpsertion,
    )(implicit
      store: StoreContext,
    ): CartItemModifierOptionUpdate =
    CartItemModifierOptionUpdate(
      id = None,
      storeId = Some(store.id),
      cartItemId = Some(itemId),
      modifierOptionId = Some(upsertion.upsertion.modifierOptionId),
      name = Some(upsertion.coreData._2.name),
      `type` = Some(upsertion.coreData._1.`type`),
      priceAmount = Some(upsertion.coreData._2.price.amount),
      quantity = upsertion.upsertion.quantity,
    )
}
