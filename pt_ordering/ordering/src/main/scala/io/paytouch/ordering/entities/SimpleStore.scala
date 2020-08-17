package io.paytouch.ordering.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.ImageUrls

final case class SimpleStore(
    id: UUID,
    locationId: UUID,
    merchantId: UUID,
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
  )
