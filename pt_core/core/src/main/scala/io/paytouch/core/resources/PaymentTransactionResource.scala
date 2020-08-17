package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.core.services.PaymentTransactionService

trait PaymentTransactionResource extends JsonResource {

  def paymentTransactionService: PaymentTransactionService

  lazy val paymentTransactionRoutes: Route =
    path("payment_transactions.validate") {
      get {
        parameters("payment_transaction_id".as[UUID]) { id =>
          authenticate { implicit user =>
            onSuccess(paymentTransactionService.validate(id))(result => completeAsEmptyResponse(result))
          }
        }
      }
    }
}
