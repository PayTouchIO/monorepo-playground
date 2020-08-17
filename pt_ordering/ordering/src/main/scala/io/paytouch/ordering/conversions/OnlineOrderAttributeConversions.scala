package io.paytouch.ordering.conversions

import io.paytouch.ordering.clients.paytouch.core.entities._
import io.paytouch.ordering.clients.paytouch.core.entities.enums.AcceptanceStatus
import io.paytouch.ordering.entities.{ Cart => CartEntity }

trait OnlineOrderAttributeConversions {

  protected def toOnlineOrderAttributeUpsertion(cart: CartEntity): OnlineOrderAttributeUpsertion =
    OnlineOrderAttributeUpsertion(
      email = cart.email,
      firstName = cart.deliveryAddress.firstName,
      lastName = cart.deliveryAddress.lastName,
      phoneNumber = cart.phoneNumber,
      prepareByTime = cart.prepareBy,
      acceptanceStatus = AcceptanceStatus.Open,
    )

}
