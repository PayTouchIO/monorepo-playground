package io.paytouch.core.entities

import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class Supplier(
    id: UUID,
    name: String,
    contact: Option[String],
    address: Option[String],
    secondaryAddress: Option[String],
    email: Option[String],
    phoneNumber: Option[String],
    secondaryPhoneNumber: Option[String],
    accountNumber: Option[String],
    notes: Option[String],
    productsCount: Option[Int],
    stockValue: Option[MonetaryAmount],
    locationOverrides: Option[Map[UUID, ItemLocation]],
  ) extends ExposedEntity {
  val classShortName = ExposedName.Supplier
}

final case class SupplierInfo(id: UUID, name: String)

final case class SupplierCreation(
    name: String,
    contact: Option[String],
    address: Option[String],
    secondaryAddress: Option[String],
    email: Option[String],
    phoneNumber: Option[String],
    secondaryPhoneNumber: Option[String],
    accountNumber: Option[String],
    notes: Option[String],
    locationOverrides: Map[UUID, Option[ItemLocationUpdate]] = Map.empty,
    productIds: Seq[UUID],
  ) extends CreationEntity[Supplier, SupplierUpdate] {

  def asUpdate: SupplierUpdate =
    SupplierUpdate(
      name = Some(name),
      contact = contact,
      address = address,
      secondaryAddress = secondaryAddress,
      email = email,
      phoneNumber = phoneNumber,
      secondaryPhoneNumber = secondaryPhoneNumber,
      accountNumber = accountNumber,
      notes = notes,
      locationOverrides = locationOverrides,
      productIds = Some(productIds),
    )
}

final case class SupplierUpdate(
    name: Option[String],
    contact: Option[String],
    address: Option[String],
    secondaryAddress: Option[String],
    email: Option[String],
    phoneNumber: Option[String],
    secondaryPhoneNumber: Option[String],
    accountNumber: Option[String],
    notes: Option[String],
    locationOverrides: Map[UUID, Option[ItemLocationUpdate]] = Map.empty,
    productIds: Option[Seq[UUID]],
  ) extends UpdateEntity[Supplier]
