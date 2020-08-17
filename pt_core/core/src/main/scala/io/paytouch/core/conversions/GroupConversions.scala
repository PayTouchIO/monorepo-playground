package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.data.model.{ GroupRecord, GroupUpdate }
import io.paytouch.core.entities.{
  CustomerMerchant,
  MonetaryAmount,
  UserContext,
  Group => GroupEntity,
  GroupUpdate => GroupUpdateEntity,
}

trait GroupConversions extends EntityConversion[GroupRecord, GroupEntity] {

  def fromRecordToEntity(record: GroupRecord)(implicit user: UserContext): GroupEntity =
    fromRecordAndOptionsToEntity(record, None, None, None, None)

  def fromRecordsAndOptionsToEntities(
      groups: Seq[GroupRecord],
      customersPerGroup: Option[Map[GroupRecord, Seq[CustomerMerchant]]],
      customersCountPerGroup: Option[Map[GroupRecord, Int]],
      revenuesPerGroup: Option[Map[GroupRecord, Seq[MonetaryAmount]]],
      visitsPerGroup: Option[Map[GroupRecord, Int]],
    ) =
    groups.map { group =>
      val customers = customersPerGroup.map(_.getOrElse(group, Seq.empty))
      val customersCount = customersCountPerGroup.map(_.getOrElse(group, 0))
      val revenues = revenuesPerGroup.map(_.getOrElse(group, Seq.empty))
      val visits = visitsPerGroup.map(_.getOrElse(group, 0))
      fromRecordAndOptionsToEntity(group, customers, customersCount, revenues, visits)
    }

  def fromRecordAndOptionsToEntity(
      record: GroupRecord,
      customers: Option[Seq[CustomerMerchant]],
      customersCount: Option[Int],
      revenues: Option[Seq[MonetaryAmount]],
      visits: Option[Int],
    ): GroupEntity =
    GroupEntity(
      id = record.id,
      name = record.name,
      customers = customers,
      customersCount = customersCount,
      revenues = revenues,
      visits = visits,
    )

  def fromUpsertionToUpdate(id: UUID, groupUpdate: GroupUpdateEntity)(implicit user: UserContext): GroupUpdate =
    GroupUpdate(id = Some(id), merchantId = Some(user.merchantId), name = groupUpdate.name)
}
