package io.paytouch.ordering.conversions

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.entities.{ AddressUpsertion, Cart => CartEntity }

trait OrderDeliveryAddressConversions {

  protected def toDeliveryAddressUpsertion(cart: CartEntity): DeliveryAddressUpsertion =
    DeliveryAddressUpsertion(
      firstName = cart.deliveryAddress.firstName,
      lastName = cart.deliveryAddress.lastName,
      address = AddressUpsertion(
        line1 = cart.deliveryAddress.address.line1,
        line2 = cart.deliveryAddress.address.line2,
        city = cart.deliveryAddress.address.city,
        state = cart.deliveryAddress.address.state,
        country = cart.deliveryAddress.address.country,
        postalCode = cart.deliveryAddress.address.postalCode,
      ),
      drivingDistanceInMeters = cart.drivingDistanceInMeters,
      estimatedDrivingTimeInMins = cart.estimatedDrivingTimeInMins,
    )
}
