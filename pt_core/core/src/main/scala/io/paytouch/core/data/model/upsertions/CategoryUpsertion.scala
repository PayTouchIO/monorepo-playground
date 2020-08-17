package io.paytouch.core.data.model.upsertions

import java.util.UUID

import io.paytouch.core.data.model._

final case class CategoryUpsertion(
    category: CategoryUpdate,
    subcategories: Seq[CategoryUpdate],
    categoryLocations: Map[UUID, Option[CategoryLocationUpdate]],
    availabilities: Seq[AvailabilityUpdate],
    locationAvailabilities: Seq[AvailabilityUpdate],
    imageUploads: Option[Seq[ImageUploadUpdate]],
  ) extends UpsertionModel[CategoryRecord]
