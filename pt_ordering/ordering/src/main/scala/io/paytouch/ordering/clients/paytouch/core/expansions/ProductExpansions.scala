package io.paytouch.ordering.clients.paytouch.core.expansions

import enumeratum._
import io.paytouch.ordering.clients.paytouch.core.Parameters
import io.paytouch.ordering.entities.enums.EnumEntrySnake
import sangria.schema.ProjectedName

sealed abstract class ProductExpansion extends EnumEntrySnake

case object ProductExpansion extends Enum[ProductExpansion] {
  case object CatalogCategoryOptions extends ProductExpansion
  case object CatalogCategoryPositions extends ProductExpansion
  case object CategoryPositions extends ProductExpansion
  case object ModifierPositions extends ProductExpansion
  case object ModifierIds extends ProductExpansion
  case object Modifiers extends ProductExpansion
  case object PriceRanges extends ProductExpansion
  case object StockLevel extends ProductExpansion
  case object TaxRateLocations extends ProductExpansion
  case object TaxRates extends ProductExpansion
  case object Variants extends ProductExpansion

  val values = findValues
}

final case class ProductExpansions(val expansions: Set[ProductExpansion]) extends Parameters {
  import ProductExpansion._
  private def add(more: ProductExpansion*): ProductExpansions = ProductExpansions(expansions ++ more)

  def withCategoryData =
    add(CatalogCategoryOptions, CatalogCategoryPositions, CategoryPositions)
  def withModifiers = add(ModifierIds, Modifiers, ModifierPositions)
  def withStockLevel = add(StockLevel)
  def withTaxRates = add(TaxRates, TaxRateLocations)
  def withVariants = add(Variants)

  def contains(item: ProductExpansion): Boolean = expansions.contains(item)
  def ++(other: ProductExpansions): ProductExpansions = add(other.expansions.toSeq: _*)

  def toParameter = expandParameter(expansions.toSeq.map(_.entryName).sorted: _*)

  def convertFieldToExpansion(entityField: String): ProductExpansions =
    entityField match {
      case "catalog_category_positions" => add(CatalogCategoryPositions)
      case "category_options"           => add(CatalogCategoryOptions)
      case "category_positions"         => add(CategoryPositions)
      case "variant_products"           => add(Variants)
      case "modifiers"                  => add(ModifierIds)
      case "modifier_positions"         => add(ModifierPositions)
      case "location_overrides"         => add(StockLevel, TaxRateLocations, TaxRates)
      case "price_range"                => add(PriceRanges)
      case _                            => this
    }

}

object ProductExpansions {
  def empty: ProductExpansions = ProductExpansions(Set.empty)

  def fromProjectionToExpansion(initial: ProductExpansions, projection: Vector[ProjectedName]) =
    projection.foldLeft(initial) {
      case (expansion, ProjectedName(name, _)) => expansion.convertFieldToExpansion(name)
    }
}
