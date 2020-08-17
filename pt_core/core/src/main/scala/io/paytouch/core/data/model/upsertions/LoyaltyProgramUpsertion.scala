package io.paytouch.core.data.model.upsertions

import io.paytouch.core.data.model._

final case class LoyaltyProgramUpsertion(
    loyaltyProgram: LoyaltyProgramUpdate,
    loyaltyProgramLocations: Seq[LoyaltyProgramLocationUpdate],
    loyaltyRewards: Option[Seq[LoyaltyRewardUpdate]],
    imageUploads: Option[Seq[ImageUploadUpdate]],
  ) extends UpsertionModel[LoyaltyProgramRecord]
