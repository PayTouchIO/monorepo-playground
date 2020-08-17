package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.ArticleRecord
import io.paytouch.core.data.model.enums.{ ArticleScope, ArticleType, UnitType }
import shapeless.{ Generic, HNil }
import slickless._

class ArticlesTable(tag: Tag) extends SlickSoftDeleteTable[ArticleRecord](tag, "products") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")
  def `type` = column[ArticleType]("type")
  def isCombo = column[Boolean]("is_combo")
  def name = column[String]("name")
  def description = column[Option[String]]("description")
  def brandId = column[Option[UUID]]("brand_id")

  def priceAmount = column[BigDecimal]("price_amount")
  def costAmount = column[Option[BigDecimal]]("cost_amount")
  def averageCostAmount = column[Option[BigDecimal]]("avg_cost_amount")
  def unit = column[UnitType]("unit")
  def margin = column[Option[BigDecimal]]("margin")

  def upc = column[Option[String]]("upc")
  def sku = column[Option[String]]("sku")

  def isVariantOfProductId = column[Option[UUID]]("is_variant_of_product_id")
  def hasVariants = column[Boolean]("has_variants")
  def trackInventory = column[Boolean]("track_inventory")
  def active = column[Boolean]("active")
  def applyPricingToAllLocations = column[Boolean]("apply_pricing_to_all_locations")
  def discountable = column[Boolean]("discountable")

  def avatarBgColor = column[Option[String]]("avatar_bg_color")

  def isService = column[Boolean]("is_service")
  def orderRoutingBar = column[Boolean]("order_routing_bar")
  def orderRoutingKitchen = column[Boolean]("order_routing_kitchen")
  def orderRoutingEnabled = column[Boolean]("order_routing_enabled")

  def trackInventoryParts = column[Boolean]("track_inventory_parts", O.Default(false))
  def hasParts = column[Boolean]("has_parts", O.Default(false))
  def scope = column[ArticleScope]("scope", O.Default(ArticleScope.Product))
  def deletedAt = column[Option[ZonedDateTime]]("deleted_at")
  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * = {
    val productGeneric = Generic[ArticleRecord]
    (id :: merchantId :: `type` :: isCombo ::
      name :: description :: brandId ::
      priceAmount :: costAmount :: averageCostAmount :: unit :: margin ::
      upc :: sku ::
      isVariantOfProductId :: hasVariants :: trackInventory :: active ::
      applyPricingToAllLocations :: discountable :: avatarBgColor ::
      isService :: orderRoutingBar :: orderRoutingKitchen :: orderRoutingEnabled ::
      trackInventoryParts :: hasParts :: scope ::
      deletedAt :: createdAt :: updatedAt :: HNil).<>(
      (dbRow: productGeneric.Repr) => productGeneric.from(dbRow),
      (caseClass: ArticleRecord) => Some(productGeneric.to(caseClass)),
    )
  }
}
