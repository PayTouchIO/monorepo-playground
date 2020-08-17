package io.paytouch.core.resources.paymentTransactions

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import io.paytouch.core.utils.{ UtcTime, FixtureDaoFactory => Factory }

class PaymentTransactionsGetFSpec extends PaymentTransactionsFSpec {

  abstract class PaymentTransactionsGetFSpecContext extends PaymentTransactionsResourceFSpecContext

  "GET /v1/payment_transactions.validate?payment_transaction_id=$" in {
    "if the payment transaction does not exist" should {
      "return 404" in new PaymentTransactionsGetFSpecContext {
        Get(s"/v1/payment_transactions.validate?payment_transaction_id=${UUID.randomUUID}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NotFound)
        }
      }
    }

    "if the payment transaction is expired" should {
      "return 400" in new PaymentTransactionsGetFSpecContext {
        val order = Factory.order(merchant).create
        val paymentTransaction = Factory.paymentTransaction(order, paidAt = Some(UtcTime.now.minusHours(49))).create

        Get(s"/v1/payment_transactions.validate?payment_transaction_id=${paymentTransaction.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.BadRequest)
        }
      }
    }

    "if the payment transaction is valid" should {
      "return 204" in new PaymentTransactionsGetFSpecContext {
        val order = Factory.order(merchant).create
        val paymentTransaction = Factory.paymentTransaction(order, paidAt = Some(UtcTime.now.minusHours(47))).create

        Get(s"/v1/payment_transactions.validate?payment_transaction_id=${paymentTransaction.id}")
          .addHeader(authorizationHeader) ~> routes ~> check {
          assertStatus(StatusCodes.NoContent)
        }
      }
    }
  }
}
