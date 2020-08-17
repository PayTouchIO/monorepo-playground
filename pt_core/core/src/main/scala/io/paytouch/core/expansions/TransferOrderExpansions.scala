package io.paytouch.core.expansions

final case class TransferOrderExpansions(
    withFromLocation: Boolean = false,
    withToLocation: Boolean = false,
    withUser: Boolean = false,
    withProductsCount: Boolean = false,
    withStockValue: Boolean = false,
  ) extends BaseExpansions

object TransferOrderExpansions {

  def forGet(
      withFromLocation: Boolean,
      withToLocation: Boolean,
      withUser: Boolean,
    ) =
    TransferOrderExpansions(withFromLocation = withFromLocation, withToLocation = withToLocation, withUser = withUser)

  def forList = apply _
}
