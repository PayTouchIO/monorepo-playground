package io.paytouch.ordering.conversions

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.VariantOptionWithType
import io.paytouch.ordering.data.model.{ CartItemVariantOptionRecord, CartItemVariantOptionUpdate }
import io.paytouch.ordering.entities.{
  AppContext,
  StoreContext,
  ValidCartItemUpsertion,
  CartItemVariantOption => CartItemVariantOptionEntity,
}

trait CartItemVariantOptionConversions {

  protected def fromRecordToEntity(
      record: CartItemVariantOptionRecord,
    )(implicit
      app: AppContext,
    ): CartItemVariantOptionEntity =
    CartItemVariantOptionEntity(
      id = record.id,
      variantOptionId = record.variantOptionId,
      optionName = record.optionName,
      optionTypeName = record.optionTypeName,
    )

  protected def toItemVariantOptionUpdateModels(
      itemId: UUID,
      upsertion: ValidCartItemUpsertion,
    )(implicit
      store: StoreContext,
    ): Option[Seq[CartItemVariantOptionUpdate]] =
    upsertion.coreData.map(_.options.map(toItemVariantOptionUpdateModel(itemId, _)))

  private def toItemVariantOptionUpdateModel(
      itemId: UUID,
      variantOptionWithType: VariantOptionWithType,
    )(implicit
      store: StoreContext,
    ): CartItemVariantOptionUpdate =
    CartItemVariantOptionUpdate(
      id = None,
      storeId = Some(store.id),
      cartItemId = Some(itemId),
      variantOptionId = Some(variantOptionWithType.id),
      optionName = Some(variantOptionWithType.name),
      optionTypeName = Some(variantOptionWithType.typeName),
    )
}
