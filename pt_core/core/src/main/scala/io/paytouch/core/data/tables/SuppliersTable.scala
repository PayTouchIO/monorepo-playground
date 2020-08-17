package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.SupplierRecord
import io.paytouch.core.entities.SupplierInfo

class SuppliersTable(tag: Tag) extends SlickSoftDeleteTable[SupplierRecord](tag, "suppliers") {

  def id = column[UUID]("id", O.PrimaryKey)

  def merchantId = column[UUID]("merchant_id")

  def name = column[String]("name")
  def contact = column[Option[String]]("contact")
  def address = column[Option[String]]("address")
  def secondaryAddress = column[Option[String]]("secondary_address")
  def email = column[Option[String]]("email")
  def phoneNumber = column[Option[String]]("phone_number")
  def secondaryPhoneNumber = column[Option[String]]("secondary_phone_number")
  def accountNumber = column[Option[String]]("account_number")
  def notes = column[Option[String]]("notes")
  def deletedAt = column[Option[ZonedDateTime]]("deleted_at")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def supplierInfo = (id, name).<>(SupplierInfo.tupled, SupplierInfo.unapply)

  def * =
    (
      id,
      merchantId,
      name,
      contact,
      address,
      secondaryAddress,
      email,
      phoneNumber,
      secondaryPhoneNumber,
      accountNumber,
      notes,
      deletedAt,
      createdAt,
      updatedAt,
    ).<>(SupplierRecord.tupled, SupplierRecord.unapply)
}
