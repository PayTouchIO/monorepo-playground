package io.paytouch.core.entities

import java.util.UUID

final case class DeliveryAddress(
    id: UUID,
    firstName: Option[String],
    lastName: Option[String],
    address: Address,
    drivingDistanceInMeters: Option[BigDecimal],
    estimatedDrivingTimeInMins: Option[Int],
  )

final case class DeliveryAddressUpsertion(
    id: UUID,
    firstName: ResettableString,
    lastName: ResettableString,
    address: AddressSync = AddressSync.empty,
    drivingDistanceInMeters: ResettableBigDecimal,
    estimatedDrivingTimeInMins: ResettableInt,
  )
