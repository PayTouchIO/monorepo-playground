package io.paytouch.ordering.conversions

import java.util.UUID

import io.paytouch.ordering.data.model.{ StoreRecord, StoreUpdate => StoreUpdateModel }
import io.paytouch.ordering.entities.{
  AppContext,
  MonetaryAmount,
  SimpleStore,
  UserContext,
  Store => StoreEntity,
  StoreUpdate => StoreUpdateEntity,
}

trait StoreConversions {
  protected def fromRecordToEntity(record: StoreRecord)(implicit context: AppContext): StoreEntity =
    StoreEntity(
      id = record.id,
      locationId = record.locationId,
      merchantId = record.merchantId,
      merchantUrlSlug = "", // overridden by mandatory expander
      urlSlug = record.urlSlug,
      catalogId = record.catalogId,
      active = record.active,
      description = record.description,
      heroBgColor = record.heroBgColor,
      heroImageUrls = record.heroImageUrls,
      logoImageUrls = record.logoImageUrls,
      takeOutEnabled = record.takeOutEnabled,
      takeOutStopMinsBeforeClosing = record.takeOutStopMinsBeforeClosing,
      deliveryEnabled = record.deliveryEnabled,
      deliveryMin = MonetaryAmount.extract(record.deliveryMinAmount),
      deliveryMax = MonetaryAmount.extract(record.deliveryMaxAmount),
      deliveryMaxDistance = record.deliveryMaxDistance,
      deliveryStopMinsBeforeClosing = record.deliveryStopMinsBeforeClosing,
      deliveryFee = MonetaryAmount.extract(record.deliveryFeeAmount),
      paymentMethods = record.paymentMethods,
    )

  protected def toUpsertionModel(id: UUID, update: StoreUpdateEntity)(implicit user: UserContext): StoreUpdateModel =
    StoreUpdateModel(
      id = id,
      merchantId = Some(user.merchantId),
      locationId = update.locationId,
      currency = Some(user.currency),
      urlSlug = update.urlSlug,
      catalogId = update.catalogId,
      active = update.active,
      description = update.description,
      heroBgColor = update.heroBgColor,
      heroImageUrls = update.heroImageUrls,
      logoImageUrls = update.logoImageUrls,
      takeOutEnabled = update.takeOutEnabled,
      takeOutStopMinsBeforeClosing = update.takeOutStopMinsBeforeClosing,
      deliveryEnabled = update.deliveryEnabled,
      deliveryMinAmount = update.deliveryMinAmount,
      deliveryMaxAmount = update.deliveryMaxAmount,
      deliveryMaxDistance = update.deliveryMaxDistance,
      deliveryStopMinsBeforeClosing = update.deliveryStopMinsBeforeClosing,
      deliveryFeeAmount = update.deliveryFeeAmount,
      paymentMethods = update.paymentMethods,
    )

  protected def fromRecordsToSimpleStores(records: Seq[StoreRecord]): Seq[SimpleStore] =
    records.map(fromRecordToSimpleStore)

  private def fromRecordToSimpleStore(record: StoreRecord): SimpleStore =
    SimpleStore(
      id = record.id,
      locationId = record.locationId,
      merchantId = record.merchantId,
      urlSlug = record.urlSlug,
      catalogId = record.catalogId,
      active = record.active,
      description = record.description,
      heroBgColor = record.heroBgColor,
      heroImageUrls = record.heroImageUrls,
      logoImageUrls = record.logoImageUrls,
      takeOutEnabled = record.takeOutEnabled,
      takeOutStopMinsBeforeClosing = record.takeOutStopMinsBeforeClosing,
      deliveryEnabled = record.deliveryEnabled,
      deliveryMin = MonetaryAmount.extract(record.deliveryMinAmount, record.currency),
      deliveryMax = MonetaryAmount.extract(record.deliveryMaxAmount, record.currency),
      deliveryMaxDistance = record.deliveryMaxDistance,
      deliveryStopMinsBeforeClosing = record.deliveryStopMinsBeforeClosing,
      deliveryFee = MonetaryAmount.extract(record.deliveryFeeAmount, record.currency),
      paymentMethods = record.paymentMethods,
    )
}
