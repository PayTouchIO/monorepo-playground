package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ StockRecord, StockUpdate => StockUpdateModel }
import io.paytouch.core.entities.{ UserContext, Stock => StockEntity, StockUpdate => StockUpdateEntity }

trait StockConversions
    extends EntityConversion[StockRecord, StockEntity]
       with ModelWithIdConversion[StockUpdateEntity, StockUpdateModel] {

  def fromRecordToEntity(record: StockRecord)(implicit user: UserContext): StockEntity =
    StockEntity(
      id = record.id,
      productId = record.productId,
      locationId = record.locationId,
      quantity = record.quantity,
      minimumOnHand = record.minimumOnHand,
      reorderAmount = record.reorderAmount,
      sellOutOfStock = record.sellOutOfStock,
    )

  def fromUpdateEntityToModel(upsertion: StockUpdateEntity)(implicit user: UserContext): StockUpdateModel =
    StockUpdateModel(
      id = None,
      merchantId = Some(user.merchantId),
      productId = Some(upsertion.productId),
      locationId = Some(upsertion.locationId),
      quantity = upsertion.quantity,
      minimumOnHand = upsertion.minimumOnHand,
      reorderAmount = upsertion.reorderAmount,
      sellOutOfStock = upsertion.sellOutOfStock,
    )

  def toEmptyStockUpdateModel(productId: UUID, locationId: UUID)(implicit user: UserContext): StockUpdateModel =
    StockUpdateModel(
      id = None,
      merchantId = Some(user.merchantId),
      productId = Some(productId),
      locationId = Some(locationId),
      quantity = Some(0),
      minimumOnHand = None,
      reorderAmount = None,
      sellOutOfStock = None,
    )
}
