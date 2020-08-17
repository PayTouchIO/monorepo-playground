package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

final case class SupplierRecord(
    id: UUID,
    merchantId: UUID,
    name: String,
    contact: Option[String],
    address: Option[String],
    secondaryAddress: Option[String],
    email: Option[String],
    phoneNumber: Option[String],
    secondaryPhoneNumber: Option[String],
    accountNumber: Option[String],
    notes: Option[String],
    deletedAt: Option[ZonedDateTime],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickSoftDeleteRecord

case class SupplierUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    name: Option[String],
    contact: Option[String],
    address: Option[String],
    secondaryAddress: Option[String],
    email: Option[String],
    phoneNumber: Option[String],
    secondaryPhoneNumber: Option[String],
    accountNumber: Option[String],
    notes: Option[String],
    deletedAt: Option[ZonedDateTime],
  ) extends SlickSoftDeleteUpdate[SupplierRecord] {

  def toRecord: SupplierRecord = {
    require(merchantId.isDefined, s"Impossible to convert SupplierUpdate without a merchant id. [$this]")
    require(name.isDefined, s"Impossible to convert SupplierUpdate without a name. [$this]")
    SupplierRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      name = name.get,
      contact = contact,
      address = address,
      secondaryAddress = secondaryAddress,
      email = email,
      phoneNumber = phoneNumber,
      secondaryPhoneNumber = secondaryPhoneNumber,
      accountNumber = accountNumber,
      notes = notes,
      deletedAt = deletedAt,
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: SupplierRecord): SupplierRecord =
    SupplierRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      name = name.getOrElse(record.name),
      contact = contact.orElse(record.contact),
      address = address.orElse(record.address),
      secondaryAddress = secondaryAddress.orElse(record.secondaryAddress),
      email = email.orElse(record.email),
      phoneNumber = phoneNumber.orElse(record.phoneNumber),
      secondaryPhoneNumber = secondaryPhoneNumber.orElse(record.secondaryPhoneNumber),
      accountNumber = accountNumber.orElse(record.accountNumber),
      notes = notes.orElse(record.notes),
      deletedAt = deletedAt.orElse(record.deletedAt),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}
