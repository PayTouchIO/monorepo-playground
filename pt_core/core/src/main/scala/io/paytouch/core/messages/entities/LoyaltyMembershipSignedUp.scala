package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName
import io.paytouch.core.entities.{ LoyaltyMembership, LoyaltyProgram, Merchant }

final case class LoyaltyMembershipSignedUp(eventName: String, payload: LoyaltyMembershipSignedUpPayload)
    extends PtNotifierMsg[LoyaltyMembershipSignUpEmail]

final case class LoyaltyMembershipSignedUpPayload(
    `object`: ExposedName,
    recipientEmail: String,
    data: LoyaltyMembershipSignUpEmail,
    merchantId: UUID,
  ) extends EntityPayloadLike[LoyaltyMembershipSignUpEmail]

final case class LoyaltyMembershipSignUpEmail(
    loyaltyMembership: LoyaltyMembership,
    merchant: Merchant,
    loyaltyProgram: LoyaltyProgram,
    barcodeUrl: String,
  )
object LoyaltyMembershipSignedUp {

  val eventName = "loyalty_membership_signed_up"

  def apply(
      recipientEmail: String,
      loyaltyMembership: LoyaltyMembership,
      merchant: Merchant,
      loyaltyProgram: LoyaltyProgram,
      barcodeUrl: String,
    ): LoyaltyMembershipSignedUp = {
    val signUpEmailData = LoyaltyMembershipSignUpEmail(loyaltyMembership, merchant, loyaltyProgram, barcodeUrl)
    val payload =
      LoyaltyMembershipSignedUpPayload(loyaltyMembership.classShortName, recipientEmail, signUpEmailData, merchant.id)
    LoyaltyMembershipSignedUp(eventName, payload)
  }
}
