package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ ModifierOptionRecord, ModifierOptionUpdate, ModifierSetRecord }
import io.paytouch.core.entities.{ MonetaryAmount, UserContext, ModifierOption => ModifierOptionEntity }

trait ModifierOptionConversions {
  def fromRecordsToEntities(
      modifierSetRecord: ModifierSetRecord,
    )(
      records: Seq[ModifierOptionRecord],
    )(implicit
      user: UserContext,
    ): Seq[ModifierOptionEntity] =
    records.map(fromRecordToEntity(modifierSetRecord))

  def fromRecordToEntity(
      modifierSetRecord: ModifierSetRecord,
    )(
      record: ModifierOptionRecord,
    )(implicit
      user: UserContext,
    ): ModifierOptionEntity =
    ModifierOptionEntity(
      id = record.id,
      name = record.name,
      price = MonetaryAmount(record.priceAmount),
      maximumCount = modifierSetRecord.maximumSingleOptionCount,
      position = record.position,
      active = record.active,
    )

  def toModelOptionUpdate(
      modifierSetId: UUID,
      modifierOption: ModifierOptionEntity,
    )(implicit
      user: UserContext,
    ): ModifierOptionUpdate =
    ModifierOptionUpdate(
      id = Some(modifierOption.id),
      merchantId = Some(user.merchantId),
      modifierSetId = Some(modifierSetId),
      name = Some(modifierOption.name),
      priceAmount = Some(modifierOption.price.amount),
      position = Some(modifierOption.position),
      active = Some(modifierOption.active),
    )
}
