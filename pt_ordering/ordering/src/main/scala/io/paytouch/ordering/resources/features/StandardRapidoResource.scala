package io.paytouch.ordering.resources.features

import java.util.UUID

import akka.http.scaladsl.server.{ Route, ValidationRejection }
import io.paytouch.ordering.entities.RapidoOrderContext

import scala.concurrent.Future

trait StandardRapidoResource extends StandardResource {

  type Context = RapidoOrderContext

  protected def contextAuthentication(f: Context => Route): Route =
    parameters("merchant_id".as[UUID], "order_id".as[UUID]) {
      case (merchantId, orderId) =>
        contextAuthenticationFromRapidoOrder(merchantId, orderId)(f)
    }

  protected def contextAuthenticationFromRapidoOrder(merchantId: UUID, orderId: UUID)(f: Context => Route): Route =
    onSuccess(authenticationService.getRapidoOrderContext(merchantId, orderId)) {
      case None          => reject(ValidationRejection(s"Order does not exist"))
      case Some(context) => f(context)
    }
}
