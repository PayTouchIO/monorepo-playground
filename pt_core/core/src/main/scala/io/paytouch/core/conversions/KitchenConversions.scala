package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ KitchenRecord, KitchenUpdate => KitchenUpdateModel }
import io.paytouch.core.entities.{ UserContext, Kitchen => KitchenEntity, KitchenUpdate => KitchenUpdateEntity }

trait KitchenConversions
    extends EntityConversion[KitchenRecord, KitchenEntity]
       with ModelConversion[KitchenUpdateEntity, KitchenUpdateModel] {

  def fromRecordToEntity(record: KitchenRecord)(implicit user: UserContext): KitchenEntity =
    KitchenEntity(
      id = record.id,
      locationId = record.locationId,
      name = record.name,
      `type` = record.`type`,
      active = record.active,
      kdsEnabled = record.kdsEnabled,
    )

  def fromUpsertionToUpdate(id: UUID, update: KitchenUpdateEntity)(implicit user: UserContext): KitchenUpdateModel =
    KitchenUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      locationId = update.locationId,
      name = update.name,
      `type` = update.`type`,
      active = update.active,
      kdsEnabled = update.kdsEnabled,
      deletedAt = None,
    )
}
