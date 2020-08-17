package io.paytouch.ordering.clients.worldpay

import com.softwaremill.macwire._
import com.softwaremill.sttp._
import com.softwaremill.sttp.testing._

import io.paytouch.ordering.clients.worldpay.entities._
import io.paytouch.ordering.entities.Cart
import io.paytouch.ordering.data.model.WorldpayConfig
import io.paytouch.ordering.utils.{ CommonArbitraries, FSpec, FixturesSupport, MockedRestApi }

import org.specs2.specification.Scope

import scala.concurrent.{ ExecutionContext, Future }

class TransactionSetupSpec extends WorldpayClientSpec {
  "WorldpayClient" should {
    "TransactionSetup" should {
      "handle a successful response" in new ClientSpecContext {
        implicit val backend = stubResponse("/worldpay/responses/transaction_setup_success.xml")

        val response = client.transactionSetup(config, orderId, total, storeName).await
        response ==== TransactionSetupResponse(transactionSetupId = "15BC256A-896F-4A3F-9455-043B30937AE6")
      }

      "handle a invalid request response" in new ClientSpecContext {
        implicit val backend = stubResponse("/worldpay/responses/invalid_request_failure.xml")

        val response = client.transactionSetup(config, orderId, total, storeName).await
        response ==== ErrorResponse(ResponseCode.InvalidRequest)
      }

      "handle a request failure" in new ClientSpecContext {
        implicit val backend = SttpBackendStub.synchronous.whenRequestMatches(_ => true).thenRespondServerError()

        val response = client.transactionSetup(config, orderId, total, storeName).await
        response ==== ErrorResponse(ResponseCode.UnknownError)
      }
    }
  }
}
