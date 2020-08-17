package io.paytouch.core.resources

import java.util.UUID

import akka.http.scaladsl.server.Route

import io.paytouch._

import io.paytouch.core.services.StripeService

trait StripeResource extends JsonResource {
  def stripeService: StripeService

  val stripeRoutes: Route =
    concat(
      path("vendor" / "stripe" / "payment_transactions.refund") {
        post {
          parameters(
            "payment_transaction_id".as[UUID],
            "amount".as[BigDecimal].?,
          ) {
            case (paymentTransactionId, amount) =>
              authenticate(implicit user =>
                onSuccess(stripeService.paymentTransactionRefund(paymentTransactionId, amount)) {
                  completeAsValidatedApiResponse
                },
              )
          }
        }
      },
      path("vendor" / "stripe" / "connect_callback") {
        post {
          parameters("code".as[String]) {
            case (code) =>
              authenticate(implicit user =>
                onSuccess(
                  stripeService.connectCallback(
                    StripeService.ConnectRequest(StripeService.Code(code)),
                  ),
                )(completeAsEmptyResponse),
              )
          }
        }
      },
    )
}
