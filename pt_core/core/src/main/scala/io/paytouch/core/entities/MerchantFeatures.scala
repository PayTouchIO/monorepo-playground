package io.paytouch.core.entities

import io.paytouch.core.entities.enums.ExposedName

final case class MerchantFeature(enabled: Boolean = true) {
  def ++(other: MerchantFeature) = MerchantFeature(enabled || other.enabled)
}

final case class MerchantFeatures(
    pos: MerchantFeature = MerchantFeature(),
    sales: MerchantFeature = MerchantFeature(),
    reports: MerchantFeature = MerchantFeature(),
    giftCards: MerchantFeature = MerchantFeature(),
    inventory: MerchantFeature = MerchantFeature(),
    tables: MerchantFeature = MerchantFeature(),
    employees: MerchantFeature = MerchantFeature(),
    customers: MerchantFeature = MerchantFeature(),
    coupons: MerchantFeature = MerchantFeature(),
    loyalty: MerchantFeature = MerchantFeature(),
    engagement: MerchantFeature = MerchantFeature(),
    onlineStore: MerchantFeature = MerchantFeature(),
  ) extends ExposedEntity {
  val classShortName = ExposedName.MerchantFeatures

  def ++(other: MerchantFeatures): MerchantFeatures =
    MerchantFeatures.create(
      pos = pos ++ other.pos,
      sales = sales ++ other.sales,
      reports = reports ++ other.reports,
      giftCards = giftCards ++ other.giftCards,
      inventory = inventory ++ other.inventory,
      tables = tables ++ other.tables,
      employees = employees ++ other.employees,
      customers = customers ++ other.customers,
      coupons = coupons ++ other.coupons,
      loyalty = loyalty ++ other.loyalty,
      engagement = engagement ++ other.engagement,
      onlineStore = onlineStore ++ other.onlineStore,
    )
}

object MerchantFeatures {
  def create(
      pos: MerchantFeature,
      sales: MerchantFeature,
      reports: MerchantFeature,
      giftCards: MerchantFeature,
      inventory: MerchantFeature,
      tables: MerchantFeature,
      employees: MerchantFeature,
      customers: MerchantFeature,
      coupons: MerchantFeature,
      loyalty: MerchantFeature,
      engagement: MerchantFeature,
      onlineStore: MerchantFeature,
    ): MerchantFeatures =
    MerchantFeatures(
      pos = pos,
      sales = sales,
      reports = reports,
      giftCards = giftCards,
      inventory = inventory,
      tables = tables,
      employees = employees,
      customers = customers,
      coupons = coupons,
      loyalty = loyalty,
      engagement = engagement,
      onlineStore = onlineStore,
    )

  def allTrue =
    MerchantFeatures.create(
      pos = MerchantFeature(enabled = true),
      sales = MerchantFeature(enabled = true),
      reports = MerchantFeature(enabled = true),
      giftCards = MerchantFeature(enabled = true),
      inventory = MerchantFeature(enabled = true),
      tables = MerchantFeature(enabled = true),
      employees = MerchantFeature(enabled = true),
      customers = MerchantFeature(enabled = true),
      coupons = MerchantFeature(enabled = true),
      loyalty = MerchantFeature(enabled = true),
      engagement = MerchantFeature(enabled = true),
      onlineStore = MerchantFeature(enabled = true),
    )
}

final case class MerchantFeaturesUpsertion(
    pos: Option[MerchantFeature] = None,
    sales: Option[MerchantFeature] = None,
    reports: Option[MerchantFeature] = None,
    giftCards: Option[MerchantFeature] = None,
    inventory: Option[MerchantFeature] = None,
    tables: Option[MerchantFeature] = None,
    employees: Option[MerchantFeature] = None,
    customers: Option[MerchantFeature] = None,
    coupons: Option[MerchantFeature] = None,
    loyalty: Option[MerchantFeature] = None,
    engagement: Option[MerchantFeature] = None,
    onlineStore: Option[MerchantFeature] = None,
  )
