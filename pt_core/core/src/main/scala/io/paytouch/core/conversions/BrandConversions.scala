package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ BrandRecord, BrandUpdate => BrandUpdateModel }
import io.paytouch.core.entities.{ UserContext, Brand => BrandEntity, BrandUpdate => BrandUpdateEntity }

trait BrandConversions
    extends EntityConversion[BrandRecord, BrandEntity]
       with ModelConversion[BrandUpdateEntity, BrandUpdateModel] {

  def fromRecordToEntity(record: BrandRecord)(implicit user: UserContext): BrandEntity =
    BrandEntity(id = record.id, name = record.name)

  def fromUpsertionToUpdate(id: UUID, update: BrandUpdateEntity)(implicit user: UserContext): BrandUpdateModel =
    BrandUpdateModel(id = Some(id), merchantId = Some(user.merchantId), name = update.name)
}
