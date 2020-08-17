package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.Day

final case class Category(
    id: UUID,
    name: String,
    catalogId: UUID,
    merchantId: UUID,
    description: Option[String],
    avatarBgColor: Option[String],
    avatarImageUrls: Seq[ImageUrls],
    position: Int,
    active: Option[Boolean],
    subcategories: Seq[Category],
    locationOverrides: Option[Map[UUID, CategoryLocation]],
    availabilities: Option[Map[Day, Seq[Availability]]],
  )

final case class CategoryLocation(active: Boolean, availabilities: Map[Day, Seq[Availability]])
