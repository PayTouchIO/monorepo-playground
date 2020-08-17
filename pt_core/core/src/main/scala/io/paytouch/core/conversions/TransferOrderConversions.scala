package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ TransferOrderRecord, TransferOrderUpdate => TransferOrderUpdateModel }
import io.paytouch.core.entities.{
  Location,
  MonetaryAmount,
  UserContext,
  UserInfo,
  TransferOrder => TransferOrderEntity,
  TransferOrderUpdate => TransferOrderUpdateEntity,
}

trait TransferOrderConversions
    extends EntityConversion[TransferOrderRecord, TransferOrderEntity]
       with ModelConversion[TransferOrderUpdateEntity, TransferOrderUpdateModel] {

  def fromRecordToEntity(transferOrder: TransferOrderRecord)(implicit user: UserContext): TransferOrderEntity =
    fromRecordAndOptionsToEntity(transferOrder, None, None, None, None, None)

  def fromRecordsAndOptionsToEntities(
      records: Seq[TransferOrderRecord],
      fromLocationPerRecord: Option[Map[TransferOrderRecord, Location]],
      toLocationPerRecord: Option[Map[TransferOrderRecord, Location]],
      userPerRecord: Option[Map[TransferOrderRecord, UserInfo]],
      productCountPerRecord: Option[Map[TransferOrderRecord, BigDecimal]],
      stockValuePerRecord: Option[Map[TransferOrderRecord, MonetaryAmount]],
    ): Seq[TransferOrderEntity] =
    records.map { record =>
      val fromLocation = fromLocationPerRecord.flatMap(_.get(record))
      val toLocation = toLocationPerRecord.flatMap(_.get(record))
      val user = userPerRecord.flatMap(_.get(record))
      val productCount = productCountPerRecord.flatMap(_.get(record))
      val stockValue = stockValuePerRecord.flatMap(_.get(record))
      fromRecordAndOptionsToEntity(record, fromLocation, toLocation, user, productCount, stockValue)
    }

  def fromRecordAndOptionsToEntity(
      record: TransferOrderRecord,
      fromLocation: Option[Location],
      toLocation: Option[Location],
      user: Option[UserInfo],
      productCount: Option[BigDecimal],
      stockValue: Option[MonetaryAmount],
    ) =
    TransferOrderEntity(
      id = record.id,
      fromLocation = fromLocation,
      toLocation = toLocation,
      user = user,
      number = record.number,
      notes = record.notes,
      status = record.status,
      `type` = record.`type`,
      productsCount = productCount,
      stockValue = stockValue,
      createdAt = record.createdAt,
      updatedAt = record.updatedAt,
    )

  def fromUpsertionToUpdate(
      id: UUID,
      update: TransferOrderUpdateEntity,
    )(implicit
      user: UserContext,
    ): TransferOrderUpdateModel =
    TransferOrderUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      fromLocationId = update.fromLocationId,
      toLocationId = update.toLocationId,
      userId = update.userId,
      notes = update.notes,
      `type` = update.`type`,
    )

}
