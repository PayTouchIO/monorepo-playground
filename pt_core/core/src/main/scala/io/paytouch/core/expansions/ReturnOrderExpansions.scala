package io.paytouch.core.expansions

final case class ReturnOrderExpansions(
    withSupplier: Boolean = false,
    withUser: Boolean = false,
    withStockValue: Boolean = false,
    withLocation: Boolean = false,
    withProductsCount: Boolean = false,
    withPurchaseOrder: Boolean = false,
  ) extends BaseExpansions

object ReturnOrderExpansions {

  def forGet(
      withSupplier: Boolean,
      withUser: Boolean,
      withLocation: Boolean,
      withProductsCount: Boolean,
      withPurchaseOrder: Boolean,
    ): ReturnOrderExpansions =
    apply(
      withSupplier = withSupplier,
      withUser = withUser,
      withStockValue = false,
      withLocation = withLocation,
      withProductsCount = withProductsCount,
      withPurchaseOrder = withPurchaseOrder,
    )

  def forList = apply _
}
