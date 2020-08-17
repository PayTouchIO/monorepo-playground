package io.paytouch.core.expansions

final case class PurchaseOrderExpansions(
    withReceivingOrders: Boolean = false,
    withSupplier: Boolean = false,
    withLocation: Boolean = false,
    withUser: Boolean = false,
    withOrderedProductsCount: Boolean = false,
    withReceivedProductsCount: Boolean = false,
    withReturnedProductsCount: Boolean = false,
  ) extends BaseExpansions

object PurchaseOrderExpansions {

  def withSupplier: PurchaseOrderExpansions =
    apply(withSupplier = true)

  def forGet(
      withReceivingOrders: Boolean,
      withSupplier: Boolean,
      withLocation: Boolean,
      withUser: Boolean,
    ) =
    apply(
      withReceivingOrders = withReceivingOrders,
      withSupplier = withSupplier,
      withLocation = withLocation,
      withUser = withUser,
    )

  def forList(
      withReceivingOrders: Boolean,
      withSupplier: Boolean,
      withOrderedProductsCount: Boolean,
      withReceivedProductsCount: Boolean,
      withReturnedProductsCount: Boolean,
    ) =
    apply(
      withReceivingOrders = withReceivingOrders,
      withSupplier = withSupplier,
      withOrderedProductsCount = withOrderedProductsCount,
      withReceivedProductsCount = withReceivedProductsCount,
      withReturnedProductsCount = withReturnedProductsCount,
    )
}
