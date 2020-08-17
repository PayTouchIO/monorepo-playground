package io.paytouch.core.conversions

import io.paytouch.core.data.model.enums.ChangeReason
import io.paytouch.core.data.model.{ ProductCostHistoryRecord, ProductCostHistoryUpdate, ProductLocationRecord }
import io.paytouch.core.entities._
import io.paytouch.core.utils.UtcTime

trait ProductCostHistoryConversions {

  def fromRecordsToCostEntities(
      records: Seq[ProductCostHistoryRecord],
      locations: Map[ProductCostHistoryRecord, Location],
      users: Map[ProductCostHistoryRecord, UserInfo],
    )(implicit
      userContext: UserContext,
    ): Seq[ProductCostHistory] =
    records.flatMap { record =>
      for {
        location <- locations.get(record)
        user <- users.get(record)
      } yield fromRecordToCostEntity(record, location, user)
    }

  def fromRecordToCostEntity(
      record: ProductCostHistoryRecord,
      location: Location,
      user: UserInfo,
    )(implicit
      userContext: UserContext,
    ): ProductCostHistory =
    ProductCostHistory(
      id = record.id,
      location = location,
      timestamp = record.date,
      prevCost = MonetaryAmount(record.prevCostAmount),
      newCost = MonetaryAmount(record.newCostAmount),
      costChange = MonetaryAmount(record.newCostAmount - record.prevCostAmount),
      reason = record.reason,
      user = user,
      notes = record.notes,
    )

  def toProductCostHistoryUpdate(
      productLocation: ProductLocationRecord,
      locationOverrideUpdate: ArticleLocationUpdate,
      reason: ChangeReason,
      notes: Option[String],
    )(implicit
      user: UserContext,
    ): ProductCostHistoryUpdate =
    ProductCostHistoryUpdate(
      id = None,
      merchantId = Some(productLocation.merchantId),
      productId = Some(productLocation.productId),
      locationId = Some(productLocation.locationId),
      userId = Some(user.id),
      date = Some(UtcTime.now),
      prevCostAmount = productLocation.costAmount,
      newCostAmount = locationOverrideUpdate.cost,
      reason = Some(reason),
      notes = notes,
    )
}
