package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities._
import io.paytouch.core.entities.enums.ExposedName

final case class PrepareLoyaltyMembershipSignedUp(eventName: String, payload: PrepareLoyaltyMembershipSignedUpPayload)
    extends PtCoreMsg[PrepareLoyaltyMembershipSignUpEmail]

final case class PrepareLoyaltyMembershipSignedUpPayload(
    `object`: ExposedName,
    data: PrepareLoyaltyMembershipSignUpEmail,
    merchantId: UUID,
    merchantContext: MerchantContext,
  ) extends EntityPayloadLike[PrepareLoyaltyMembershipSignUpEmail]

final case class PrepareLoyaltyMembershipSignUpEmail(
    loyaltyMembership: LoyaltyMembership,
    loyaltyProgram: LoyaltyProgram,
  )

object PrepareLoyaltyMembershipSignedUp {

  val eventName = "prepare_loyalty_membership_signed_up"

  def apply(
      loyaltyMembership: LoyaltyMembership,
      loyaltyProgram: LoyaltyProgram,
    )(implicit
      merchantContext: MerchantContext,
    ): PrepareLoyaltyMembershipSignedUp = {
    val signUpEmailData = PrepareLoyaltyMembershipSignUpEmail(loyaltyMembership, loyaltyProgram)
    val payload = PrepareLoyaltyMembershipSignedUpPayload(
      loyaltyMembership.classShortName,
      signUpEmailData,
      merchantContext.id,
      merchantContext,
    )
    PrepareLoyaltyMembershipSignedUp(eventName, payload)
  }
}
