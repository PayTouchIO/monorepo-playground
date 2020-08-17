package io.paytouch.core.data.model.upsertions

import java.util.UUID

import io.paytouch.core.data.model._

final case class ModifierSetUpsertion(
    modifierSet: ModifierSetUpdate,
    modifierSetLocations: Map[UUID, Option[ModifierSetLocationUpdate]],
    options: Option[Seq[ModifierOptionUpdate]],
  ) extends UpsertionModel[ModifierSetRecord]
