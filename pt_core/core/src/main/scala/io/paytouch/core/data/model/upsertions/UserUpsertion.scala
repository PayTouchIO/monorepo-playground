package io.paytouch.core.data.model.upsertions

import java.util.UUID

import io.paytouch.core.data.model._

final case class UserUpsertion(
    user: UserUpdate,
    userLocations: Map[UUID, Option[UserLocationUpdate]],
    imageUploadUpdates: Option[Seq[ImageUploadUpdate]],
  ) extends UpsertionModel[UserRecord]
