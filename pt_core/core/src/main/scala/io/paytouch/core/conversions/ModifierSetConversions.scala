package io.paytouch.core.conversions

import java.util.UUID

import cats.implicits._

import io.paytouch._

import io.paytouch.core.LocationOverridesPer
import io.paytouch.core.data.model.{ ModifierSetUpdate => ModifierSetUpdateModel, _ }
import io.paytouch.core.entities.{ ModifierSet => ModifierSetEntity, ModifierSetUpdate => ModifierSetUpdateEntity, _ }

trait ModifierSetConversions
    extends EntityConversion[ModifierSetRecord, ModifierSetEntity]
       with ModelConversion[ModifierSetUpdateEntity, ModifierSetUpdateModel] {
  def fromRecordsAndOptionsToEntities(
      modifierSets: Seq[ModifierSetRecord],
      modifierOptions: Option[Map[ModifierSetRecord, Seq[ModifierOption]]],
      countsPerModifierSet: Option[Map[ModifierSetRecord, Int]],
      locationsOverridesPerModifierSet: Option[LocationOverridesPer[ModifierSetRecord, ItemLocation]],
    ) =
    modifierSets.map { modifierSet =>
      val options = modifierOptions.map(_.getOrElse(modifierSet, Seq.empty))
      val productsCount = countsPerModifierSet.map(_.getOrElse(modifierSet, 0))
      val locationOverrides = locationsOverridesPerModifierSet.map(_.getOrElse(modifierSet, Map.empty))
      fromRecordAndOptionsToEntity(modifierSet, options, productsCount, locationOverrides)
    }

  def fromRecordToEntity(modifierSet: ModifierSetRecord)(implicit user: UserContext): ModifierSetEntity =
    fromRecordAndOptionsToEntity(modifierSet, None, None, None)

  def fromRecordAndOptionsToEntity(
      modifierSet: ModifierSetRecord,
      modifierOptions: Option[Seq[ModifierOption]],
      productsCount: Option[Int],
      locationOverrides: Option[Map[UUID, ItemLocation]],
    ): ModifierSetEntity =
    ModifierSetEntity(
      id = modifierSet.id,
      `type` = modifierSet.`type`,
      name = modifierSet.name,
      minimumOptionCount = modifierSet.optionCount.minimum.value.some,
      maximumOptionCount = modifierSet.optionCount.maximum.map(_.value).some,
      maximumSingleOptionCount = modifierSet.maximumSingleOptionCount,
      singleChoice = modifierSet.optionCount.singleChoice.some,
      force = modifierSet.optionCount.force.some,
      hideOnReceipts = modifierSet.hideOnReceipts,
      locationOverrides = locationOverrides,
      options = modifierOptions,
      productsCount = productsCount,
    )

  final override def fromUpsertionToUpdate(
      id: UUID,
      upsertion: ModifierSetUpdateEntity,
    )(implicit
      user: UserContext,
    ): ModifierSetUpdateModel =
    sys.error("for some reason I was forced to override this")

  def fromUpsertionToUpdateWithProperTypes(
      id: UUID,
      update: ModifierSetUpdateEntity,
      optionCount: Option[ModifierOptionCount],
    )(implicit
      user: UserContext,
    ): ModifierSetUpdateModel =
    ModifierSetUpdateModel(
      id = Some(id),
      `type` = update.`type`,
      merchantId = Some(user.merchantId),
      name = update.name,
      optionCount = optionCount,
      maximumSingleOptionCount = update.maximumSingleOptionCount,
      hideOnReceipts = update.hideOnReceipts,
    )

  def groupModifierSetsPerProduct(
      modifierSetProducts: Seq[ModifierSetProductRecord],
      modifierSets: Seq[ModifierSetEntity],
    ): Map[UUID, Seq[ModifierSetEntity]] =
    modifierSetProducts.groupBy(_.productId).transform { (_, modSetPrds) =>
      modSetPrds.flatMap(modifierSetProduct => modifierSets.find(_.id == modifierSetProduct.modifierSetId))
    }
}
