package io.paytouch.core.conversions

import io.paytouch.core.data.model.enums.ChangeReason
import io.paytouch.core.data.model.{ ProductLocationRecord, ProductPriceHistoryRecord, ProductPriceHistoryUpdate }
import io.paytouch.core.entities._
import io.paytouch.core.utils.UtcTime

trait ProductPriceHistoryConversions {

  def fromRecordsToPriceEntities(
      records: Seq[ProductPriceHistoryRecord],
      locations: Map[ProductPriceHistoryRecord, Location],
      users: Map[ProductPriceHistoryRecord, UserInfo],
    )(implicit
      userContext: UserContext,
    ): Seq[ProductPriceHistory] =
    records.flatMap { record =>
      for {
        location <- locations.get(record)
        user <- users.get(record)
      } yield fromRecordToPriceEntity(record, location, user)
    }

  def fromRecordToPriceEntity(
      record: ProductPriceHistoryRecord,
      location: Location,
      user: UserInfo,
    )(implicit
      userContext: UserContext,
    ): ProductPriceHistory =
    ProductPriceHistory(
      id = record.id,
      location = location,
      timestamp = record.date,
      prevPrice = MonetaryAmount(record.prevPriceAmount),
      newPrice = MonetaryAmount(record.newPriceAmount),
      priceChange = MonetaryAmount(record.newPriceAmount - record.prevPriceAmount),
      reason = record.reason,
      user = user,
      notes = record.notes,
    )

  def toProductPriceHistoryUpdate(
      productLocation: ProductLocationRecord,
      locationOverrideUpdate: ArticleLocationUpdate,
      reason: ChangeReason,
      notes: Option[String],
    )(implicit
      user: UserContext,
    ): ProductPriceHistoryUpdate =
    ProductPriceHistoryUpdate(
      id = None,
      merchantId = Some(productLocation.merchantId),
      productId = Some(productLocation.productId),
      locationId = Some(productLocation.locationId),
      userId = Some(user.id),
      date = Some(UtcTime.now),
      prevPriceAmount = Some(productLocation.priceAmount),
      newPriceAmount = Some(locationOverrideUpdate.price),
      reason = Some(reason),
      notes = notes,
    )

}
