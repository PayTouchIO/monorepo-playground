package io.paytouch.core.entities

import java.util.UUID

import io.scalaland.chimney.dsl._

import io.paytouch.core.entities.enums.ExposedName

final case class ModifierOption(
    id: UUID,
    name: String,
    price: MonetaryAmount,
    maximumCount: Option[Int],
    position: Int,
    active: Boolean,
  ) extends ExposedEntity {
  val classShortName = ExposedName.ModifierOption
}

final case class ModifierOptionUpsertion(
    id: UUID,
    name: String,
    price: MonetaryAmount,
    position: Int,
    active: Boolean,
  ) {
  def toModifierOption: ModifierOption =
    this
      .into[ModifierOption]
      .enableOptionDefaultsToNone
      .transform
}
