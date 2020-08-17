package io.paytouch.ordering.clients.worldpay

import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType
import io.paytouch.ordering.clients.worldpay.entities._
import com.softwaremill.sttp.testing._

import scala.concurrent.{ ExecutionContext, Future }

class TransactionQuerySpec extends WorldpayClientSpec {
  "WorldpayClient" should {
    "TransactionQuery" should {
      val transactionSetupId = "transactionSetupId"

      "handle a successful response" in new ClientSpecContext {
        implicit val backend = stubResponse("/worldpay/responses/transaction_query_success.xml")

        val response = client.transactionQuery(config, transactionSetupId).await
        response ==== TransactionQueryResponse(
          accountId = "1066410",
          applicationId = "10025",
          approvalNumber = "651831",
          approvedAmount = 12.00,
          maskedCardNumber = "************0006",
          cardType = CardType.Visa,
          cardHolderName = "VANTIV TEST EMV 606- PayTouch",
          terminalId = "0060810007",
          transactionId = "19365463",
          transactionSetupId = "CD54D147-069B-4073-B357-7F0DDEDA577A",
          hostResponseCode = "00",
        )
      }

      "handle a successful response with multiple transactions" in new ClientSpecContext {
        implicit val backend = stubResponse("/worldpay/responses/transaction_query_success_multiple.xml")

        val response = client.transactionQuery(config, transactionSetupId).await
        response ==== TransactionQueryResponse(
          accountId = "1066410",
          applicationId = "10025",
          approvalNumber = "907613",
          approvedAmount = 17.50,
          maskedCardNumber = "************0006",
          cardType = CardType.Visa,
          cardHolderName = "VANTIV TEST EMV 606- PayTouch",
          terminalId = "0060810007",
          transactionId = "21989266",
          transactionSetupId = "04BB79E8-17ED-4FF0-AA12-534D05D238BB",
          hostResponseCode = "00",
        )
      }

      "handle a no record response" in new ClientSpecContext {
        implicit val backend = stubResponse("/worldpay/responses/transaction_query_no_record.xml")

        val response = client.transactionQuery(config, transactionSetupId).await
        response ==== ErrorResponse(ResponseCode.NoRecord)
      }

      "handle a invalid request response" in new ClientSpecContext {
        implicit val backend = stubResponse("/worldpay/responses/invalid_request_failure.xml")

        val response = client.transactionQuery(config, transactionSetupId).await
        response ==== ErrorResponse(ResponseCode.InvalidRequest)
      }

      "handle a request failure" in new ClientSpecContext {
        implicit val backend = SttpBackendStub.synchronous.whenRequestMatches(_ => true).thenRespondServerError()

        val response = client.transactionQuery(config, transactionSetupId).await
        response ==== ErrorResponse(ResponseCode.UnknownError)
      }
    }
  }
}
