package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.{ ArticleScope, ArticleType }
import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.entities.enums.UnitType

final case class Product(
    id: UUID,
    `type`: ArticleType,
    scope: ArticleScope,
    isCombo: Boolean,
    name: String,
    description: Option[String],
    price: MonetaryAmount,
    variants: Option[Seq[VariantOptionType]],
    variantProducts: Option[Seq[Product]],
    unit: UnitType,
    isVariantOfProductId: Option[UUID],
    hasVariant: Boolean,
    active: Boolean,
    locationOverrides: Map[UUID, ProductLocation],
    options: Seq[VariantOptionWithType],
    modifierIds: Option[Seq[UUID]],
    modifierPositions: Option[Seq[ModifierPosition]],
    modifiers: Option[Seq[ModifierSet]],
    avatarBgColor: Option[String],
    avatarImageUrls: Seq[ImageUrls],
    hasParts: Boolean,
    trackInventory: Boolean,
    categoryOptions: Option[Seq[CategoryOption]],
    categoryPositions: Seq[CategoryPosition],
    bundleSets: Seq[BundleSet],
    priceRange: Option[MonetaryRange],
  )
