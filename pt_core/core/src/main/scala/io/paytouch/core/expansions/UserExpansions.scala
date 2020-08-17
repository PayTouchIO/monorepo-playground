package io.paytouch.core.expansions

final case class UserExpansions(
    withLocations: Boolean,
    withMerchant: Boolean,
    withMerchantSetupSteps: Boolean,
    withPermissions: Boolean,
    withAccess: Boolean,
    withMerchantLegalDetails: Boolean,
  ) extends BaseExpansions {
  def asUserRoleExpansions =
    UserRoleExpansions(withUsersCount = false, withPermissions = withPermissions || withAccess)
}

object UserExpansions {
  def withoutPermissions(
      withLocations: Boolean,
      withMerchant: Boolean,
      withMerchantSetupSteps: Boolean,
      withAccess: Boolean,
      withMerchantLegalDetails: Boolean,
    ): UserExpansions =
    UserExpansions(
      withLocations = withLocations,
      withMerchant = withMerchant,
      withMerchantSetupSteps = withMerchantSetupSteps,
      withPermissions = false,
      withAccess = withAccess,
      withMerchantLegalDetails = withMerchantLegalDetails,
    )

  def withPermissions(
      withLocations: Boolean,
      withMerchant: Boolean,
      withMerchantSetupSteps: Boolean,
      withAccess: Boolean,
      withMerchantLegalDetails: Boolean,
    ): UserExpansions =
    UserExpansions(
      withLocations = withLocations,
      withMerchant = withMerchant,
      withMerchantSetupSteps = withMerchantSetupSteps,
      withPermissions = true,
      withAccess = withAccess,
      withMerchantLegalDetails = withMerchantLegalDetails,
    )

  def empty =
    UserExpansions(
      withLocations = false,
      withMerchant = false,
      withMerchantSetupSteps = false,
      withPermissions = false,
      withAccess = false,
      withMerchantLegalDetails = false,
    )
}
