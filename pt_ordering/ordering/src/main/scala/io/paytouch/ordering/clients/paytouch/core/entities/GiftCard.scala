package io.paytouch.ordering.clients.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.ordering.entities._
import io.paytouch.ordering.entities.enums.ExposedName

final case class GiftCard(
    id: UUID,
    amounts: Seq[MonetaryAmount],
    businessName: String,
    templateDetails: Option[String],
    templateCreated: Boolean,
    active: Boolean,
    product: Product,
    avatarImageUrls: Seq[ImageUrls],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.GiftCard
}

final case class GiftCardCreation(
    amounts: Seq[BigDecimal],
    businessName: String,
    templateDetails: Option[String],
    active: Option[Boolean],
    name: String,
    upc: ResettableString,
    sku: ResettableString,
    imageUploadIds: Option[Seq[UUID]],
  ) extends CreationEntity[GiftCardUpdate] {
  def asUpsert: GiftCardUpdate =
    GiftCardUpdate(
      amounts = Some(amounts),
      businessName = Some(businessName),
      templateDetails = templateDetails,
      active = active,
      name = Some(name),
      upc = upc,
      sku = sku,
      imageUploadIds = imageUploadIds,
    )
}

final case class GiftCardUpdate(
    amounts: Option[Seq[BigDecimal]],
    businessName: Option[String],
    templateDetails: Option[String],
    active: Option[Boolean],
    name: Option[String],
    upc: ResettableString,
    sku: ResettableString,
    imageUploadIds: Option[Seq[UUID]],
  )

object GiftCardUpdate {
  def empty: GiftCardUpdate =
    GiftCardUpdate(
      amounts = None,
      businessName = None,
      templateDetails = None,
      active = None,
      name = None,
      upc = None,
      sku = None,
      imageUploadIds = None,
    )
}
