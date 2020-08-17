package io.paytouch.core.conversions

import io.paytouch.core.calculations.ProductStocksCalculations
import io.paytouch.core.data.model.ProductQuantityHistoryRecord
import io.paytouch.core.entities.{ StockUpdate => StockUpdateEntity, _ }

trait ProductQuantityHistoryConversions extends ProductStocksCalculations {

  def fromRecordsToEntities(
      records: Seq[ProductQuantityHistoryRecord],
      locations: Map[ProductQuantityHistoryRecord, Location],
      users: Map[ProductQuantityHistoryRecord, UserInfo],
    )(implicit
      userContext: UserContext,
    ): Seq[ProductQuantityHistory] =
    records.flatMap { record =>
      locations.get(record).map(location => fromRecordToEntity(record, location, users.get(record)))
    }

  def fromRecordToEntity(
      record: ProductQuantityHistoryRecord,
      location: Location,
      user: Option[UserInfo],
    )(implicit
      userContext: UserContext,
    ): ProductQuantityHistory =
    ProductQuantityHistory(
      id = record.id,
      location = location,
      timestamp = record.date,
      prevQuantity = record.prevQuantityAmount,
      newQuantity = record.newQuantityAmount,
      newStockValue = MonetaryAmount(record.newStockValueAmount),
      reason = record.reason,
      user = user,
      notes = record.notes,
    )
}
