package io.paytouch.ordering.entities

import java.util.{ Currency, UUID }

import akka.http.scaladsl.model.headers.Authorization
import io.paytouch.ordering.clients.paytouch.core.entities.{ CoreUserContext, Order }
import io.paytouch.ordering.data.model.StoreRecord

sealed trait AppContext {
  def currency: Currency

  def contextIds: Seq[UUID]

  def merchantId: UUID
}

final case class UserContext(
    id: UUID,
    merchantId: UUID,
    locationIds: Seq[UUID],
    currency: Currency,
    authToken: Authorization,
  ) extends AppContext {
  val contextIds = locationIds
}

object UserContext {
  def apply(coreContext: CoreUserContext, authToken: Authorization): UserContext =
    apply(coreContext.id, coreContext.merchantId, coreContext.locationIds, coreContext.currency, authToken)
}

final case class StoreContext(
    id: UUID,
    currency: Currency,
    merchantId: UUID,
    locationId: UUID,
    deliveryFeeAmount: Option[BigDecimal],
    deliveryMinAmount: Option[BigDecimal],
    deliveryMaxAmount: Option[BigDecimal],
    paymentMethods: Seq[PaymentMethod],
  ) extends AppContext {
  val contextIds = Seq(id)
}

object StoreContext {
  def fromRecord(store: StoreRecord): StoreContext =
    StoreContext(
      store.id,
      store.currency,
      store.merchantId,
      store.locationId,
      store.deliveryFeeAmount,
      store.deliveryMinAmount,
      store.deliveryMaxAmount,
      store.paymentMethods,
    )
}

final case class RapidoOrderContext(
    merchantId: UUID,
    order: Order,
    currency: Currency,
  ) extends AppContext {
  val contextIds = Seq(merchantId, order.id)
}

object RapidoOrderContext {
  def apply(merchantId: UUID, order: Order): RapidoOrderContext = {
    // There should always be a total on the order
    val currency = order.total.get.currency
    apply(merchantId, order, currency)
  }
}
