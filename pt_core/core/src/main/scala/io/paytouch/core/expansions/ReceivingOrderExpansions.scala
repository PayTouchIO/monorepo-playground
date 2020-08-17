package io.paytouch.core.expansions

final case class ReceivingOrderExpansions(
    withLocation: Boolean = false,
    withPurchaseOrder: Boolean = false,
    withTransferOrder: Boolean = false,
    withUser: Boolean = false,
    withProductsCount: Boolean = false,
    withStockValue: Boolean = false,
  ) extends BaseExpansions

object ReceivingOrderExpansions {
  def forGet(
      withLocation: Boolean,
      withPurchaseOrder: Boolean,
      withTransferOrder: Boolean,
      withUser: Boolean,
    ) =
    ReceivingOrderExpansions(
      withLocation = withLocation,
      withPurchaseOrder = withPurchaseOrder,
      withTransferOrder = withTransferOrder,
      withUser = withUser,
    )

  def forList(
      withProductsCount: Boolean,
      withStockValue: Boolean,
      withUser: Boolean,
      withPurchaseOrder: Boolean,
      withTransferOrder: Boolean,
    ) =
    ReceivingOrderExpansions(
      withProductsCount = withProductsCount,
      withStockValue = withStockValue,
      withUser = withUser,
      withPurchaseOrder = withPurchaseOrder,
      withTransferOrder = withTransferOrder,
    )

  val withPurchaseOrTransferOrderAndLocation =
    ReceivingOrderExpansions(withPurchaseOrder = true, withLocation = true, withTransferOrder = true)
}
