package io.paytouch.core.conversions

import java.util.UUID

import io.paytouch.core.LocationOverridesPer
import io.paytouch.core.data.model.{ SupplierRecord, SupplierUpdate => SupplierUpdateModel }
import io.paytouch.core.entities.{
  ItemLocation,
  MonetaryAmount,
  UserContext,
  Supplier => SupplierEntity,
  SupplierUpdate => SupplierUpdateEntity,
}

trait SupplierConversions
    extends EntityConversion[SupplierRecord, SupplierEntity]
       with ModelConversion[SupplierUpdateEntity, SupplierUpdateModel] {

  def fromRecordToEntity(record: SupplierRecord)(implicit user: UserContext): SupplierEntity =
    fromRecordAndOptionsToEntity(record, None, None, None)

  def fromRecordsAndOptionsToEntities(
      records: Seq[SupplierRecord],
      productsCountPerSupplier: Map[SupplierRecord, Int],
      stockValuesPerSupplier: Map[SupplierRecord, MonetaryAmount],
      locationsOverridesPerSupplier: Option[LocationOverridesPer[SupplierRecord, ItemLocation]],
    ) =
    records.map { record =>
      val productsCount = productsCountPerSupplier.get(record)
      val stockValues = stockValuesPerSupplier.get(record)
      val locationOverrides = locationsOverridesPerSupplier.map(_.getOrElse(record, Map.empty))
      fromRecordAndOptionsToEntity(record, productsCount, stockValues, locationOverrides)
    }

  def fromRecordAndOptionsToEntity(
      record: SupplierRecord,
      productsCount: Option[Int],
      stockValue: Option[MonetaryAmount],
      locationOverrides: Option[Map[UUID, ItemLocation]],
    ): SupplierEntity =
    SupplierEntity(
      id = record.id,
      name = record.name,
      contact = record.contact,
      address = record.address,
      secondaryAddress = record.secondaryAddress,
      email = record.email,
      phoneNumber = record.phoneNumber,
      secondaryPhoneNumber = record.secondaryPhoneNumber,
      accountNumber = record.accountNumber,
      notes = record.notes,
      productsCount = productsCount,
      stockValue = stockValue,
      locationOverrides = locationOverrides,
    )

  def fromUpsertionToUpdate(
      id: UUID,
      update: SupplierUpdateEntity,
    )(implicit
      user: UserContext,
    ): SupplierUpdateModel = // TODO - change! tickets to follow
    SupplierUpdateModel(
      id = Some(id),
      merchantId = Some(user.merchantId),
      name = update.name,
      contact = update.contact,
      address = update.address,
      secondaryAddress = update.secondaryAddress,
      email = update.email,
      phoneNumber = update.phoneNumber,
      secondaryPhoneNumber = update.secondaryPhoneNumber,
      accountNumber = update.accountNumber,
      notes = update.notes,
      deletedAt = None,
    )
}
