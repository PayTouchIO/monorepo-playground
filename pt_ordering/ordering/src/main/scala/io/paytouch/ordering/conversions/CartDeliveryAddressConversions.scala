package io.paytouch.ordering.conversions

import io.paytouch.ordering.data.model.CartRecord
import io.paytouch.ordering.entities.{ Address, DeliveryAddress }

trait CartDeliveryAddressConversions {

  protected def toDeliveryAddress(record: CartRecord) =
    DeliveryAddress(
      firstName = record.firstName,
      lastName = record.lastName,
      address = toAddress(record),
    )

  private def toAddress(record: CartRecord) =
    Address(
      line1 = record.deliveryAddressLine1,
      line2 = record.deliveryAddressLine2,
      city = record.deliveryCity,
      state = record.deliveryState,
      country = record.deliveryCountry,
      postalCode = record.deliveryPostalCode,
    )
}
