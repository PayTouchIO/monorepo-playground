package io.paytouch.core.expansions

final case class SupplierExpansions(
    withLocations: Boolean,
    withProductsCount: Boolean,
    withStockValues: Boolean,
  ) extends BaseExpansions
