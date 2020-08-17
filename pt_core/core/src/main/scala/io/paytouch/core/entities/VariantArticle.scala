package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.data.model.ArticleRecord
import io.paytouch.core.data.model.enums.UnitType

final case class VariantArticleCreation(
    id: UUID,
    optionIds: Seq[UUID],
    sku: ResettableString,
    upc: ResettableString,
    cost: ResettableBigDecimal,
    price: Option[BigDecimal],
    unit: Option[UnitType],
    margin: ResettableBigDecimal,
    applyPricingToAllLocations: Option[Boolean],
    discountable: Option[Boolean],
    avatarBgColor: ResettableString,
    isService: Option[Boolean],
    orderRoutingBar: Option[Boolean],
    orderRoutingKitchen: Option[Boolean],
    orderRoutingEnabled: Option[Boolean],
    locationOverrides: Map[UUID, Option[ArticleLocationUpdate]],
  ) {
  def asUpdate(parent: ArticleCreation) =
    VariantArticleUpdate(
      id = id,
      optionIds = optionIds,
      sku = sku,
      upc = upc,
      cost = cost.getOrElse(parent.cost),
      price = price.orElse(Some(parent.price)),
      unit = unit.orElse(Some(parent.unit)),
      margin = margin.getOrElse(parent.margin),
      applyPricingToAllLocations = applyPricingToAllLocations.orElse(parent.applyPricingToAllLocations),
      discountable = discountable.orElse(parent.discountable),
      avatarBgColor = avatarBgColor.getOrElse(parent.avatarBgColor),
      isService = isService.orElse(parent.isService),
      orderRoutingBar = orderRoutingBar.orElse(parent.orderRoutingBar),
      orderRoutingKitchen = orderRoutingKitchen.orElse(parent.orderRoutingKitchen),
      orderRoutingEnabled = orderRoutingEnabled.orElse(parent.orderRoutingEnabled),
      locationOverrides = if (locationOverrides.isEmpty) parent.locationOverrides else locationOverrides,
    )
}

trait ToVariantArticleCreation[T] {

  def convert(t: T)(implicit user: UserContext): VariantArticleCreation

}

final case class VariantArticleUpdate(
    id: UUID,
    optionIds: Seq[UUID],
    sku: ResettableString,
    upc: ResettableString,
    cost: ResettableBigDecimal,
    price: Option[BigDecimal],
    unit: Option[UnitType],
    margin: ResettableBigDecimal,
    applyPricingToAllLocations: Option[Boolean],
    discountable: Option[Boolean],
    avatarBgColor: ResettableString,
    isService: Option[Boolean],
    orderRoutingBar: Option[Boolean],
    orderRoutingKitchen: Option[Boolean],
    orderRoutingEnabled: Option[Boolean],
    locationOverrides: Map[UUID, Option[ArticleLocationUpdate]],
  ) {
  def enrich(record: ArticleRecord, update: ArticleUpdate)(implicit user: UserContext) =
    this.copy(
      cost = cost.getOrElse(update.cost).orElse(record.costAmount),
      price = price.orElse(update.price).orElse(Some(record.priceAmount)),
      unit = unit.orElse(update.unit).orElse(Some(record.unit)),
      margin = margin.getOrElse(update.margin).orElse(record.margin),
      applyPricingToAllLocations = applyPricingToAllLocations
        .orElse(update.applyPricingToAllLocations)
        .orElse(Some(record.applyPricingToAllLocations)),
      discountable = discountable.orElse(update.discountable).orElse(Some(record.discountable)),
      avatarBgColor = avatarBgColor.getOrElse(update.avatarBgColor).orElse(record.avatarBgColor),
      isService = isService.orElse(update.isService).orElse(Some(record.isService)),
      orderRoutingBar = orderRoutingBar.orElse(update.orderRoutingBar).orElse(Some(record.orderRoutingBar)),
      orderRoutingKitchen =
        orderRoutingKitchen.orElse(update.orderRoutingKitchen).orElse(Some(record.orderRoutingKitchen)),
      orderRoutingEnabled =
        orderRoutingEnabled.orElse(update.orderRoutingEnabled).orElse(Some(record.orderRoutingEnabled)),
    )
}

trait ToVariantArticleUpdate[T] {

  def convert(t: T)(implicit user: UserContext): VariantArticleUpdate

}
