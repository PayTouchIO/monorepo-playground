package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class LoyaltyMembership(
    id: UUID,
    customerId: UUID,
    loyaltyProgramId: UUID,
    lookupId: String,
    points: Int,
    passPublicUrls: PassUrls,
    pointsToNextReward: Int,
    customerOptInAt: Option[ZonedDateTime],
    merchantOptInAt: Option[ZonedDateTime],
    enrolled: Boolean,
    visits: Int,
    totalSpend: MonetaryAmount,
  ) extends ExposedEntity {
  val classShortName = ExposedName.LoyaltyMembership
}
