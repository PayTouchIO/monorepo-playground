package io.paytouch.ordering.json.serializers

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.{ ArticleScope, ArticleType }
import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.entities.enums.UnitType
import org.json4s.{ CustomSerializer, Extraction, JObject, _ }

object ProductSerializer
    extends CustomSerializer[Product](implicit format =>
      (
        {
          case jsonObj: JObject =>
            val id = (jsonObj \ "id").extract[UUID]
            val `type` = (jsonObj \ "type").extract[ArticleType]
            val scope = (jsonObj \ "scope").extract[ArticleScope]
            val isCombo = (jsonObj \ "isCombo").extract[Boolean]
            val name = (jsonObj \ "name").extract[String]
            val description = (jsonObj \ "description").extract[Option[String]]
            val price = (jsonObj \ "price").extract[MonetaryAmount]
            val variants = (jsonObj \ "variants").extract[Option[Seq[VariantOptionType]]]
            val variantProducts = (jsonObj \ "variantProducts").extract[Option[Seq[Product]]]
            val unit = (jsonObj \ "unit").extract[UnitType]
            val isVariantOfProductId = (jsonObj \ "isVariantOfProductId").extract[Option[UUID]]
            val hasVariant = (jsonObj \ "hasVariant").extract[Boolean]
            val trackInventory = (jsonObj \ "trackInventory").extract[Boolean]
            val active = (jsonObj \ "active").extract[Boolean]
            val locationOverrides = (jsonObj \ "locationOverrides").extract[Map[UUID, ProductLocation]]
            val options = (jsonObj \ "options").extract[Seq[VariantOptionWithType]]
            val modifierIds = (jsonObj \ "modifierIds").extract[Option[Seq[UUID]]]
            val modifiers = (jsonObj \ "modifiers").extract[Option[Seq[ModifierSet]]]
            val modifierPositions =
              (jsonObj \ "modifierPositions").extract[Option[Seq[ModifierPosition]]]
            val avatarBgColor = (jsonObj \ "avatarBgColor").extract[Option[String]]
            val avatarImageUrls = (jsonObj \ "avatarImageUrls").extract[Seq[ImageUrls]]
            val hasParts = (jsonObj \ "hasParts").extract[Boolean]
            val categoryOptions = (jsonObj \ "catalogCategoryOptions").extract[Option[Seq[CategoryOption]]]
            val catalogCategoryPositions = (jsonObj \ "catalogCategoryPositions").extract[Option[Seq[CategoryPosition]]]
            val systemCategoryPositions = (jsonObj \ "categoryPositions").extract[Option[Seq[CategoryPosition]]]
            val bundleSets = (jsonObj \ "bundleSets").extract[Seq[BundleSet]]
            val priceRange = (jsonObj \ "priceRange").extract[Option[MonetaryRange]]

            val categoryPositions = catalogCategoryPositions.getOrElse(Seq.empty)
            Product(
              id = id,
              `type` = `type`,
              scope = scope,
              isCombo = isCombo,
              name = name,
              description = description,
              price = price,
              variants = variants,
              variantProducts = variantProducts,
              unit = unit,
              isVariantOfProductId = isVariantOfProductId,
              hasVariant = hasVariant,
              trackInventory = trackInventory,
              active = active,
              locationOverrides = locationOverrides,
              options = options,
              modifierIds = modifierIds,
              modifiers = modifiers,
              modifierPositions = modifierPositions,
              avatarBgColor = avatarBgColor,
              avatarImageUrls = avatarImageUrls,
              hasParts = hasParts,
              categoryOptions = categoryOptions,
              categoryPositions = categoryPositions,
              bundleSets = bundleSets,
              priceRange = priceRange,
            )
        },
        {
          case p: Product =>
            JObject(
              JField("id", Extraction.decompose(p.id)),
              JField("type", Extraction.decompose(p.`type`)),
              JField("scope", Extraction.decompose(p.scope)),
              JField("isCombo", Extraction.decompose(p.isCombo)),
              JField("name", Extraction.decompose(p.name)),
              JField("description", Extraction.decompose(p.description)),
              JField("price", Extraction.decompose(p.price)),
              JField("variants", Extraction.decompose(p.variants)),
              JField("variantProducts", Extraction.decompose(p.variantProducts)),
              JField("unit", Extraction.decompose(p.unit)),
              JField("isVariantOfProductId", Extraction.decompose(p.isVariantOfProductId)),
              JField("hasVariant", Extraction.decompose(p.hasVariant)),
              JField("trackInventory", Extraction.decompose(p.trackInventory)),
              JField("active", Extraction.decompose(p.active)),
              JField("locationOverrides", Extraction.decompose(p.locationOverrides)),
              JField("options", Extraction.decompose(p.options)),
              JField("modifierIds", Extraction.decompose(p.modifierIds)),
              JField("modifiers", Extraction.decompose(p.modifiers)),
              JField("modifierPositions", Extraction.decompose(p.modifierPositions)),
              JField("avatarBgColor", Extraction.decompose(p.avatarBgColor)),
              JField("avatarImageUrls", Extraction.decompose(p.avatarImageUrls)),
              JField("hasParts", Extraction.decompose(p.hasParts)),
              JField("categoryOptions", Extraction.decompose(p.categoryOptions)),
              JField("categoryPositions", Extraction.decompose(p.categoryPositions)),
              JField("bundleSets", Extraction.decompose(p.bundleSets)),
              JField("priceRange", Extraction.decompose(p.priceRange)),
            )
        },
      ),
    )
