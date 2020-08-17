package io.paytouch.ordering.entities

final case class DeliveryAddress(
    firstName: Option[String],
    lastName: Option[String],
    address: Address,
  )

final case class DeliveryAddressUpsertion(
    firstName: ResettableString = None,
    lastName: ResettableString = None,
    address: AddressUpsertion = AddressUpsertion(),
  )
