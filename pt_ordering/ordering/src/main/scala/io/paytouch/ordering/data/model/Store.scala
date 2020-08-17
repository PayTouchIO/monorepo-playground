package io.paytouch.ordering.data.model

import cats.implicits._

import java.time.ZonedDateTime
import java.util.{ Currency, UUID }

import io.paytouch.ordering.clients.paytouch.core.entities.ImageUrls
import io.paytouch.ordering.entities._

final case class StoreRecord(
    id: UUID,
    merchantId: UUID,
    locationId: UUID,
    currency: Currency,
    urlSlug: String,
    catalogId: UUID,
    active: Boolean,
    description: Option[String],
    heroBgColor: Option[String],
    heroImageUrls: Seq[ImageUrls],
    logoImageUrls: Seq[ImageUrls],
    takeOutEnabled: Boolean,
    takeOutStopMinsBeforeClosing: Option[Int],
    deliveryEnabled: Boolean,
    deliveryMinAmount: Option[BigDecimal],
    deliveryMaxAmount: Option[BigDecimal],
    deliveryMaxDistance: Option[BigDecimal],
    deliveryStopMinsBeforeClosing: Option[Int],
    deliveryFeeAmount: Option[BigDecimal],
    paymentMethods: Seq[PaymentMethod],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickLocationRecord
       with SlickToggleableRecord {
  def deriveUpdateFromPreviousState(other: StoreRecord): StoreUpdate =
    StoreUpdate(
      id = id,
      merchantId = if (merchantId != other.merchantId) merchantId.some else None,
      locationId = if (locationId != other.locationId) locationId.some else None,
      currency = if (currency != other.currency) currency.some else None,
      urlSlug = if (urlSlug != other.urlSlug) urlSlug.some else None,
      catalogId = if (catalogId != other.catalogId) catalogId.some else None,
      active = if (active != other.active) active.some else None,
      description = if (description != other.description) description else None,
      heroBgColor = if (heroBgColor != other.heroBgColor) heroBgColor else None,
      heroImageUrls = if (heroImageUrls != other.heroImageUrls) heroImageUrls.some else None,
      logoImageUrls = if (logoImageUrls != other.logoImageUrls) logoImageUrls.some else None,
      takeOutEnabled = if (takeOutEnabled != other.takeOutEnabled) takeOutEnabled.some else None,
      takeOutStopMinsBeforeClosing =
        if (takeOutStopMinsBeforeClosing != other.takeOutStopMinsBeforeClosing) takeOutStopMinsBeforeClosing else None,
      deliveryEnabled = if (deliveryEnabled != other.deliveryEnabled) deliveryEnabled.some else None,
      deliveryMinAmount = if (deliveryMinAmount != other.deliveryMinAmount) deliveryMinAmount else None,
      deliveryMaxAmount = if (deliveryMaxAmount != other.deliveryMaxAmount) deliveryMaxAmount else None,
      deliveryMaxDistance = if (deliveryMaxDistance != other.deliveryMaxDistance) deliveryMaxDistance else None,
      deliveryStopMinsBeforeClosing =
        if (deliveryStopMinsBeforeClosing != other.deliveryStopMinsBeforeClosing) deliveryStopMinsBeforeClosing
        else None,
      deliveryFeeAmount = if (deliveryFeeAmount != other.deliveryFeeAmount) deliveryFeeAmount else None,
      paymentMethods = if (paymentMethods != other.paymentMethods) paymentMethods.some else None,
    )
}

final case class StoreUpdate(
    id: Default[UUID],
    merchantId: Option[UUID],
    locationId: Option[UUID],
    currency: Option[Currency],
    urlSlug: Option[String],
    catalogId: Option[UUID],
    active: Option[Boolean],
    description: ResettableString,
    heroBgColor: ResettableString,
    heroImageUrls: Option[Seq[ImageUrls]],
    logoImageUrls: Option[Seq[ImageUrls]],
    takeOutEnabled: Option[Boolean],
    takeOutStopMinsBeforeClosing: ResettableInt,
    deliveryEnabled: Option[Boolean],
    deliveryMinAmount: ResettableBigDecimal,
    deliveryMaxAmount: ResettableBigDecimal,
    deliveryMaxDistance: ResettableBigDecimal,
    deliveryStopMinsBeforeClosing: ResettableInt,
    deliveryFeeAmount: ResettableBigDecimal,
    paymentMethods: Option[Seq[PaymentMethod]],
  ) extends SlickUpdate[StoreRecord] {
  def toRecord: StoreRecord = {
    requires(
      "merchant id" -> merchantId,
      "location id" -> locationId,
      "catalog id" -> catalogId,
      "currency" -> currency,
      "url slug" -> urlSlug,
      "active" -> active,
      "hero image urls" -> heroImageUrls,
      "logo image urls" -> logoImageUrls,
      "take out enabled" -> takeOutEnabled,
      "delivery enabled" -> deliveryEnabled,
      "payment methods" -> paymentMethods,
    )

    StoreRecord(
      id = id.getOrDefault,
      merchantId = merchantId.get,
      locationId = locationId.get,
      currency = currency.get,
      urlSlug = urlSlug.get,
      catalogId = catalogId.get,
      active = active.get,
      description = description,
      heroBgColor = heroBgColor,
      heroImageUrls = heroImageUrls.get,
      logoImageUrls = logoImageUrls.get,
      takeOutEnabled = takeOutEnabled.get,
      takeOutStopMinsBeforeClosing = takeOutStopMinsBeforeClosing,
      deliveryEnabled = deliveryEnabled.get,
      deliveryMinAmount = deliveryMinAmount,
      deliveryMaxAmount = deliveryMaxAmount,
      deliveryMaxDistance = deliveryMaxDistance,
      deliveryStopMinsBeforeClosing = deliveryStopMinsBeforeClosing,
      deliveryFeeAmount = deliveryFeeAmount,
      paymentMethods = paymentMethods.get,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: StoreRecord): StoreRecord =
    StoreRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      locationId = locationId.getOrElse(record.locationId),
      currency = currency.getOrElse(record.currency),
      urlSlug = urlSlug.getOrElse(record.urlSlug),
      catalogId = catalogId.getOrElse(record.catalogId),
      active = active.getOrElse(record.active),
      description = description.getOrElse(record.description),
      heroBgColor = heroBgColor.getOrElse(record.heroBgColor),
      heroImageUrls = heroImageUrls.getOrElse(record.heroImageUrls),
      logoImageUrls = logoImageUrls.getOrElse(record.logoImageUrls),
      takeOutEnabled = takeOutEnabled.getOrElse(record.takeOutEnabled),
      takeOutStopMinsBeforeClosing = takeOutStopMinsBeforeClosing.getOrElse(record.takeOutStopMinsBeforeClosing),
      deliveryEnabled = deliveryEnabled.getOrElse(record.deliveryEnabled),
      deliveryMinAmount = deliveryMinAmount.getOrElse(record.deliveryMinAmount),
      deliveryMaxAmount = deliveryMaxAmount.getOrElse(record.deliveryMaxAmount),
      deliveryMaxDistance = deliveryMaxDistance.getOrElse(record.deliveryMaxDistance),
      deliveryStopMinsBeforeClosing = deliveryStopMinsBeforeClosing.getOrElse(record.deliveryStopMinsBeforeClosing),
      deliveryFeeAmount = deliveryFeeAmount.getOrElse(record.deliveryFeeAmount),
      paymentMethods = paymentMethods.getOrElse(record.paymentMethods),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
