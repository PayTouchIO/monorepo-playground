package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.entities.{ AddressUpsertion, ResettableBigDecimal, ResettableInt, ResettableString }

final case class DeliveryAddressUpsertion(
    id: UUID = UUID.randomUUID,
    firstName: ResettableString,
    lastName: ResettableString,
    address: AddressUpsertion,
    drivingDistanceInMeters: ResettableBigDecimal,
    estimatedDrivingTimeInMins: ResettableInt,
  )
