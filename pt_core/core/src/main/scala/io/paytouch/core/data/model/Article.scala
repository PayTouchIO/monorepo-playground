package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, UnitType }
import io.paytouch.core.entities.{ ResettableBigDecimal, ResettableString, ResettableUUID }

final case class ArticleRecord(
    id: UUID,
    merchantId: UUID,
    `type`: ArticleType,
    isCombo: Boolean,
    name: String,
    description: Option[String],
    brandId: Option[UUID],
    priceAmount: BigDecimal,
    costAmount: Option[BigDecimal],
    averageCostAmount: Option[BigDecimal],
    unit: UnitType,
    margin: Option[BigDecimal],
    upc: Option[String],
    sku: Option[String],
    isVariantOfProductId: Option[UUID],
    hasVariant: Boolean,
    trackInventory: Boolean,
    active: Boolean,
    applyPricingToAllLocations: Boolean,
    discountable: Boolean,
    avatarBgColor: Option[String],
    isService: Boolean,
    orderRoutingBar: Boolean,
    orderRoutingKitchen: Boolean,
    orderRoutingEnabled: Boolean,
    trackInventoryParts: Boolean,
    hasParts: Boolean,
    scope: ArticleScope,
    deletedAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickSoftDeleteRecord {

  def mainProductId = isVariantOfProductId.getOrElse(id)
}

case class ArticleUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    `type`: Option[ArticleType],
    isCombo: Option[Boolean],
    name: Option[String],
    description: ResettableString,
    brandId: ResettableUUID,
    priceAmount: Option[BigDecimal],
    costAmount: ResettableBigDecimal,
    averageCostAmount: ResettableBigDecimal,
    unit: Option[UnitType],
    margin: ResettableBigDecimal,
    upc: ResettableString,
    sku: ResettableString,
    isVariantOfProductId: Option[UUID],
    hasVariant: Option[Boolean],
    trackInventory: Option[Boolean],
    active: Option[Boolean],
    applyPricingToAllLocations: Option[Boolean],
    discountable: Option[Boolean],
    avatarBgColor: ResettableString,
    isService: Option[Boolean],
    orderRoutingBar: Option[Boolean],
    orderRoutingKitchen: Option[Boolean],
    orderRoutingEnabled: Option[Boolean],
    trackInventoryParts: Option[Boolean],
    hasParts: Option[Boolean],
    scope: Option[ArticleScope],
    deletedAt: Option[ZonedDateTime],
  ) extends SlickSoftDeleteUpdate[ArticleRecord] {

  def toRecord: ArticleRecord = {
    require(merchantId.isDefined, s"Impossible to convert ProductUpdate without a merchant id. [$this]")
    require(`type`.isDefined, s"Impossible to convert ProductUpdate without a type. [$this]")
    require(name.isDefined, s"Impossible to convert ProductUpdate without a name. [$this]")
    require(priceAmount.isDefined, s"Impossible to convert ProductUpdate without a price amount. [$this]")
    require(unit.isDefined, s"Impossible to convert ProductUpdate without a unit. [$this]")
    require(scope.isDefined, s"Impossible to convert ProductUpdate without a scope. [$this]")
    ArticleRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      `type` = `type`.get,
      isCombo = isCombo.getOrElse(false),
      name = name.get,
      description = description,
      brandId = brandId,
      priceAmount = priceAmount.get,
      costAmount = costAmount,
      averageCostAmount = averageCostAmount,
      unit = unit.get,
      margin = margin,
      upc = upc,
      sku = sku,
      isVariantOfProductId = isVariantOfProductId,
      hasVariant = hasVariant.getOrElse(false),
      trackInventory = trackInventory.getOrElse(false),
      active = active.getOrElse(true),
      applyPricingToAllLocations = applyPricingToAllLocations.getOrElse(true),
      discountable = discountable.getOrElse(false),
      avatarBgColor = avatarBgColor,
      isService = isService.getOrElse(false),
      orderRoutingBar = orderRoutingBar.getOrElse(false),
      orderRoutingKitchen = orderRoutingKitchen.getOrElse(false),
      orderRoutingEnabled = orderRoutingEnabled.getOrElse(false),
      trackInventoryParts = trackInventoryParts.getOrElse(false),
      hasParts = hasParts.getOrElse(false),
      scope = scope.get,
      deletedAt = deletedAt,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: ArticleRecord): ArticleRecord =
    ArticleRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      `type` = record.`type`,
      isCombo = isCombo.getOrElse(record.isCombo),
      name = name.getOrElse(record.name),
      description = description.getOrElse(record.description),
      brandId = brandId.getOrElse(record.brandId),
      priceAmount = priceAmount.getOrElse(record.priceAmount),
      costAmount = costAmount.getOrElse(record.costAmount),
      averageCostAmount = averageCostAmount.getOrElse(record.averageCostAmount),
      unit = unit.getOrElse(record.unit),
      margin = margin.getOrElse(record.margin),
      upc = upc.getOrElse(record.upc),
      sku = sku.getOrElse(record.sku),
      isVariantOfProductId = isVariantOfProductId.orElse(record.isVariantOfProductId),
      hasVariant = hasVariant.getOrElse(record.hasVariant),
      trackInventory = trackInventory.getOrElse(record.trackInventory),
      active = active.getOrElse(record.active),
      applyPricingToAllLocations = applyPricingToAllLocations.getOrElse(record.applyPricingToAllLocations),
      discountable = discountable.getOrElse(record.discountable),
      avatarBgColor = avatarBgColor.getOrElse(record.avatarBgColor),
      isService = isService.getOrElse(record.isService),
      orderRoutingBar = orderRoutingBar.getOrElse(record.orderRoutingBar),
      orderRoutingKitchen = orderRoutingKitchen.getOrElse(record.orderRoutingKitchen),
      orderRoutingEnabled = orderRoutingEnabled.getOrElse(record.orderRoutingEnabled),
      trackInventoryParts = trackInventoryParts.getOrElse(record.trackInventoryParts),
      hasParts = hasParts.getOrElse(record.hasParts),
      scope = scope.getOrElse(record.scope),
      deletedAt = deletedAt.orElse(record.deletedAt),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object ArticleUpdate {

  def empty: ArticleUpdate =
    ArticleUpdate(
      id = None,
      merchantId = None,
      `type` = None,
      isCombo = None,
      name = None,
      description = None,
      brandId = None,
      priceAmount = None,
      costAmount = None,
      averageCostAmount = None,
      unit = None,
      margin = None,
      upc = None,
      sku = None,
      isVariantOfProductId = None,
      hasVariant = None,
      trackInventory = None,
      active = None,
      applyPricingToAllLocations = None,
      discountable = None,
      avatarBgColor = None,
      isService = None,
      orderRoutingBar = None,
      orderRoutingKitchen = None,
      orderRoutingEnabled = None,
      trackInventoryParts = None,
      hasParts = None,
      scope = None,
      deletedAt = None,
    )
}
