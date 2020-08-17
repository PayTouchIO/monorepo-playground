package io.paytouch.core.clients.urbanairship.entities

import io.paytouch.core.entities.MonetaryAmount

sealed abstract class TemplateData extends Product with Serializable
object TemplateData {
  final case class LoyaltyTemplateData(
      merchantName: String,
      iconImage: Option[String],
      logoImage: Option[String],
      address: Option[String],
      details: Option[String],
      phone: Option[String],
      website: String,
    ) extends TemplateData

  final case class GiftCardTemplateData(
      merchantName: String,
      currentBalance: MonetaryAmount,
      originalBalance: MonetaryAmount,
      lastSpend: Option[MonetaryAmount],
      address: Option[String],
      details: Option[String],
      androidImage: Option[String],
      appleFullWidthImage: Option[String],
      logoImage: Option[String],
      phone: Option[String],
      website: String,
    ) extends TemplateData
}
