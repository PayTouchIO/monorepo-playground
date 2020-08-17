package io.paytouch.core.entities

import java.util.UUID

import cats.implicits._

import io.paytouch.core.data.model.enums.ModifierSetType
import io.paytouch.core.entities.enums.ExposedName

final case class ModifierSet(
    id: UUID,
    `type`: ModifierSetType,
    name: String,
    minimumOptionCount: Option[Int],
    maximumOptionCount: Option[Option[Int]],
    maximumSingleOptionCount: Option[Int],
    singleChoice: Option[Boolean],
    force: Option[Boolean],
    hideOnReceipts: Boolean,
    locationOverrides: Option[Map[UUID, ItemLocation]],
    options: Option[Seq[ModifierOption]],
    productsCount: Option[Int],
  ) extends ExposedEntity {
  val classShortName = ExposedName.ModifierSet
}

final case class ModifierSetCreation(
    `type`: ModifierSetType,
    name: String,
    minimumOptionCount: Option[Int],
    maximumOptionCount: Option[Option[Int]],
    singleChoice: Option[Boolean],
    force: Option[Boolean],
    maximumSingleOptionCount: Option[Int],
    hideOnReceipts: Option[Boolean],
    locationOverrides: Map[UUID, Option[ItemLocationUpdate]] = Map.empty,
    options: Option[Seq[ModifierOptionUpsertion]],
  ) extends CreationEntity[ModifierSet, ModifierSetUpdate] {
  def asUpdate =
    ModifierSetUpdate(
      `type` = Some(`type`),
      name = Some(name),
      minimumOptionCount = minimumOptionCount,
      maximumOptionCount = maximumOptionCount,
      singleChoice = singleChoice.some,
      force = force.some,
      maximumSingleOptionCount = maximumSingleOptionCount.some,
      hideOnReceipts = hideOnReceipts.orElse(Some(false)),
      locationOverrides = locationOverrides,
      options = options,
    )
}

final case class ModifierSetUpdate(
    `type`: Option[ModifierSetType],
    name: Option[String],
    minimumOptionCount: Option[Int],
    maximumOptionCount: Option[Option[Int]],
    singleChoice: Option[Option[Boolean]],
    force: Option[Option[Boolean]],
    maximumSingleOptionCount: Option[Option[Int]],
    hideOnReceipts: Option[Boolean],
    locationOverrides: Map[UUID, Option[ItemLocationUpdate]] = Map.empty,
    options: Option[Seq[ModifierOptionUpsertion]],
  ) extends UpdateEntity[ModifierSet]

final case class ModifierSetProductsAssignment(productIds: Seq[UUID])
