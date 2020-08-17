package io.paytouch.ordering.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.ImageUrls
import io.paytouch.ordering.entities.enums.ExposedName

final case class Store(
    id: UUID,
    locationId: UUID,
    merchantId: UUID,
    merchantUrlSlug: String,
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
    deliveryMin: Option[MonetaryAmount],
    deliveryMax: Option[MonetaryAmount],
    deliveryMaxDistance: Option[BigDecimal],
    deliveryStopMinsBeforeClosing: Option[Int],
    deliveryFee: Option[MonetaryAmount],
    paymentMethods: Seq[PaymentMethod],
  ) extends ExposedEntity {
  val classShortName = ExposedName.Store
}

final case class StoreCreation(
    locationId: UUID,
    urlSlug: String,
    catalogId: UUID,
    active: BooleanTrue,
    description: Option[String],
    heroBgColor: Option[String],
    heroImageUrls: Seq[ImageUrls] = Seq.empty,
    logoImageUrls: Seq[ImageUrls] = Seq.empty,
    takeOutEnabled: BooleanFalse,
    takeOutStopMinsBeforeClosing: Option[Int],
    deliveryEnabled: BooleanFalse,
    deliveryMinAmount: Option[BigDecimal],
    deliveryMaxAmount: Option[BigDecimal],
    deliveryMaxDistance: Option[BigDecimal],
    deliveryStopMinsBeforeClosing: Option[Int],
    deliveryFeeAmount: Option[BigDecimal],
    paymentMethods: Seq[PaymentMethod] = Seq.empty,
  ) extends CreationEntity[StoreUpdate] {
  def asUpsert =
    StoreUpdate(
      locationId = Some(locationId),
      urlSlug = Some(urlSlug),
      catalogId = Some(catalogId),
      active = active,
      description = description,
      heroBgColor = heroBgColor,
      heroImageUrls = Some(heroImageUrls),
      logoImageUrls = Some(logoImageUrls),
      takeOutEnabled = takeOutEnabled,
      takeOutStopMinsBeforeClosing = takeOutStopMinsBeforeClosing,
      deliveryEnabled = deliveryEnabled,
      deliveryMinAmount = deliveryMinAmount,
      deliveryMaxAmount = deliveryMaxAmount,
      deliveryMaxDistance = deliveryMaxDistance,
      deliveryStopMinsBeforeClosing = deliveryStopMinsBeforeClosing,
      deliveryFeeAmount = deliveryFeeAmount,
      paymentMethods = Some(paymentMethods),
    )
}

final case class StoreUpdate(
    locationId: Option[UUID],
    urlSlug: Option[String],
    catalogId: ResettableUUID,
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
  )
