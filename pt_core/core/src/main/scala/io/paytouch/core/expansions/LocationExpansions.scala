package io.paytouch.core.expansions

final case class LocationExpansions(
    withSettings: Boolean,
    withTaxRates: Boolean,
    withOpeningHours: Boolean,
  ) extends BaseExpansions

object LocationExpansions {

  def empty = LocationExpansions(false, false, false)
  def all = LocationExpansions(true, true, true)
}
