package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.{ Availabilities, LocationOverridesPer }
import io.paytouch.core.data.model.enums.DiscountType
import io.paytouch.core.data.model.{ DiscountRecord, DiscountUpdate }
import io.paytouch.core.entities.{
  ItemLocation,
  UserContext,
  Discount => DiscountEntity,
  DiscountUpdate => DiscountUpdateEntity,
}

trait DiscountConversions
    extends EntityConversion[DiscountRecord, DiscountEntity]
       with ModelConversion[DiscountUpdateEntity, DiscountUpdate] {

  def fromRecordsAndOptionsToEntities(
      records: Seq[DiscountRecord],
      locationsOverridesPerDiscount: Option[LocationOverridesPer[DiscountRecord, ItemLocation]],
      availabilitiesPerDiscount: Option[Map[DiscountRecord, Availabilities]],
    )(implicit
      user: UserContext,
    ): Seq[DiscountEntity] =
    records.map { record =>
      val locationOverrides = locationsOverridesPerDiscount.map(_.getOrElse(record, Map.empty))
      val availabilities = availabilitiesPerDiscount.map(_.getOrElse(record, Map.empty))
      fromRecordAndOptionsToEntity(record, locationOverrides, availabilities)
    }

  def fromRecordToEntity(record: DiscountRecord)(implicit user: UserContext): DiscountEntity =
    fromRecordAndOptionsToEntity(record, None, None)

  def fromRecordAndOptionsToEntity(
      record: DiscountRecord,
      locationOverrides: Option[Map[UUID, ItemLocation]],
      availabilities: Option[Availabilities],
    )(implicit
      user: UserContext,
    ): DiscountEntity =
    DiscountEntity(
      id = record.id,
      title = record.title,
      `type` = record.`type`,
      currency = if (record.`type` == DiscountType.Percentage) None else Some(user.currency),
      amount = record.amount,
      requireManagerApproval = record.requireManagerApproval,
      locationOverrides = locationOverrides,
      availabilityHours = availabilities,
    )

  def fromUpsertionToUpdate(id: UUID, upsertion: DiscountUpdateEntity)(implicit user: UserContext): DiscountUpdate =
    DiscountUpdate(
      id = Some(id),
      merchantId = Some(user.merchantId),
      title = upsertion.title,
      `type` = upsertion.`type`,
      amount = upsertion.amount,
      requireManagerApproval = upsertion.requireManagerApproval,
    )
}
