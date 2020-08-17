package io.paytouch.core.expansions

final case class MerchantExpansions(
    withSetupSteps: Boolean,
    withOwners: Boolean,
    withLocations: Boolean,
    withLegalDetails: Boolean,
  ) extends BaseExpansions

object MerchantExpansions {
  val none: MerchantExpansions =
    MerchantExpansions(
      withSetupSteps = false,
      withOwners = false,
      withLocations = false,
      withLegalDetails = false,
    )

  val all: MerchantExpansions =
    MerchantExpansions(
      withSetupSteps = true,
      withOwners = true,
      withLocations = true,
      withLegalDetails = true,
    )
}
