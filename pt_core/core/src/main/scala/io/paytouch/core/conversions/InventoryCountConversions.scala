package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ InventoryCountRecord, InventoryCountUpdate => InventoryCountUpdateModel }
import io.paytouch.core.entities.{
  Location,
  MonetaryAmount,
  UserContext,
  UserInfo,
  InventoryCount => InventoryCountEntity,
  InventoryCountUpdate => InventoryCountUpdateEntity,
}

trait InventoryCountConversions
    extends EntityConversion[InventoryCountRecord, InventoryCountEntity]
       with ModelConversion[InventoryCountUpdateEntity, InventoryCountUpdateModel] {

  def fromRecordToEntity(record: InventoryCountRecord)(implicit user: UserContext): InventoryCountEntity =
    fromRecordAndOptionsToEntity(record, None, None, None)

  def fromRecordsAndOptionsToEntities(
      records: Seq[InventoryCountRecord],
      productsCountPerRecord: Map[InventoryCountRecord, Int],
      userInfoPerRecord: Option[Map[InventoryCountRecord, UserInfo]],
      locationPerRecord: Option[Map[InventoryCountRecord, Location]],
    )(implicit
      user: UserContext,
    ) =
    records.map { record =>
      val productsCount = productsCountPerRecord.get(record)
      val userInfo = userInfoPerRecord.flatMap(_.get(record))
      val location = locationPerRecord.flatMap(_.get(record))
      fromRecordAndOptionsToEntity(record, productsCount, userInfo, location)
    }

  def fromRecordAndOptionsToEntity(
      record: InventoryCountRecord,
      productsCount: Option[Int],
      userInfo: Option[UserInfo],
      location: Option[Location],
    )(implicit
      user: UserContext,
    ): InventoryCountEntity =
    InventoryCountEntity(
      id = record.id,
      location = location,
      user = userInfo,
      number = record.number,
      productsCount = productsCount.getOrElse(0),
      valueChange = MonetaryAmount.extract(record.valueChangeAmount),
      status = record.status,
      synced = record.synced,
      createdAt = record.createdAt,
    )

  def fromUpsertionToUpdate(id: UUID, update: InventoryCountUpdateEntity)(implicit user: UserContext) =
    InventoryCountUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      userId = Some(user.id),
      locationId = Some(update.locationId),
      valueChangeAmount = None,
      status = None,
      synced = None,
    )
}
