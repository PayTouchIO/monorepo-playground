package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ TimeOffCardRecord, TimeOffCardUpdate => TimeOffCardUpdateModel }
import io.paytouch.core.entities.{
  UserContext,
  UserInfo,
  TimeOffCard => TimeOffCardEntity,
  TimeOffCardUpdate => TimeOffCardUpdateEntity,
}

trait TimeOffCardConversions extends ModelConversion[TimeOffCardUpdateEntity, TimeOffCardUpdateModel] {

  def fromRecordsAndOptionsToEntities(records: Seq[TimeOffCardRecord], users: Map[TimeOffCardRecord, UserInfo]) =
    records.flatMap { record =>
      for {
        user <- users.get(record)
      } yield fromRecordToEntity(record, user)
    }

  def groupByUserByTimeOffCard(users: Seq[UserInfo], items: Seq[TimeOffCardRecord]): Map[TimeOffCardRecord, UserInfo] =
    items.flatMap(item => users.find(_.id == item.userId).map(user => (item, user))).toMap

  def fromRecordToEntity(record: TimeOffCardRecord, user: UserInfo): TimeOffCardEntity =
    TimeOffCardEntity(
      id = record.id,
      user = user,
      paid = record.paid,
      `type` = record.`type`,
      notes = record.notes,
      startAt = record.startAt,
      endAt = record.endAt,
    )

  def fromUpsertionToUpdate(
      id: UUID,
      upsertion: TimeOffCardUpdateEntity,
    )(implicit
      user: UserContext,
    ): TimeOffCardUpdateModel =
    TimeOffCardUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      userId = upsertion.userId,
      paid = upsertion.paid,
      `type` = upsertion.`type`,
      notes = upsertion.notes,
      startAt = upsertion.startAt,
      endAt = upsertion.endAt,
    )
}
