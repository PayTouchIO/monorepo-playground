package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{ LoyaltyMembership, MerchantContext }

final case class LoyaltyMembershipChanged(eventName: String, payload: LoyaltyMembershipPayload)
    extends PtCoreMsg[LoyaltyMembership]

object LoyaltyMembershipChanged {

  val eventName = "loyalty_membership_changed"

  def apply(loyaltyMembership: LoyaltyMembership)(implicit merchant: MerchantContext): LoyaltyMembershipChanged =
    LoyaltyMembershipChanged(eventName, LoyaltyMembershipPayload(loyaltyMembership))
}

final case class LoyaltyMembershipPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: LoyaltyMembership,
    merchantContext: MerchantContext,
  ) extends EntityPayloadLike[LoyaltyMembership]

object LoyaltyMembershipPayload {
  def apply(loyaltyMembership: LoyaltyMembership)(implicit merchant: MerchantContext): LoyaltyMembershipPayload =
    LoyaltyMembershipPayload(loyaltyMembership.classShortName, merchant.id, loyaltyMembership, merchant)
}
