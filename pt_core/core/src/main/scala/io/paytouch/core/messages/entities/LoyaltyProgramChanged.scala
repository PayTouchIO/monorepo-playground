package io.paytouch.core.messages.entities

import java.util.UUID

import io.paytouch.core.entities.LoyaltyProgram
import io.paytouch.core.entities.enums.ExposedName

final case class LoyaltyProgramChanged(eventName: String, payload: LoyaltyProgramPayload)
    extends PtCoreMsg[LoyaltyProgram]

object LoyaltyProgramChanged {

  val eventName = "loyalty_program_changed"

  def apply(merchantId: UUID, loyaltyProgram: LoyaltyProgram): LoyaltyProgramChanged =
    LoyaltyProgramChanged(eventName, LoyaltyProgramPayload(merchantId, loyaltyProgram))
}

final case class LoyaltyProgramPayload(
    `object`: ExposedName,
    merchantId: UUID,
    data: LoyaltyProgram,
  ) extends EntityPayloadLike[LoyaltyProgram]

object LoyaltyProgramPayload {
  def apply(merchantId: UUID, loyaltyProgram: LoyaltyProgram): LoyaltyProgramPayload =
    LoyaltyProgramPayload(loyaltyProgram.classShortName, merchantId, loyaltyProgram)
}
