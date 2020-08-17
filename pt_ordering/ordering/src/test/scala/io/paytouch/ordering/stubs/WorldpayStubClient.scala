package io.paytouch.ordering.stubs

import java.util.UUID

import akka.http.scaladsl.model.{ Uri => AkkaUri }
import com.softwaremill.sttp._
import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.data.model.{ CartRecord, WorldpayConfig }
import io.paytouch.ordering.clients.paytouch.core.entities.Location
import io.paytouch.ordering.clients.paytouch.core.entities.enums.CardType
import io.paytouch.ordering.clients.worldpay._
import io.paytouch.ordering.clients.worldpay.entities._
import io.paytouch.ordering.utils.CommonArbitraries
import io.paytouch.ordering._

import scala.collection._
import scala.concurrent.{ ExecutionContext, Future }

class WorldpayStubClient()(implicit ec: ExecutionContext, sttpBackend: SttpBackend[Id, Nothing])
    extends WorldpayClient(
      AkkaUri("transacation-endpoint").taggedWith[WorldpayTransactionEndpointUri],
      AkkaUri("reporting-endpoint")
        .taggedWith[WorldpayReportingEndpointUri],
      "application-id".taggedWith[WorldpayApplicationId],
      AkkaUri("return-uri").taggedWith[WorldpayReturnUri],
    ) {
  override def transactionSetup(
      config: WorldpayConfig,
      orderId: UUID,
      total: MonetaryAmount,
      storeName: Option[String],
    ): Future[WorldpayResponse] =
    Future.successful(
      WorldpayStubData.retrieveSetupResponse(),
    )

  override def transactionQuery(config: WorldpayConfig, transactionSetupId: String): Future[WorldpayResponse] =
    Future.successful {
      WorldpayStubData.retrieveQueryResponse(transactionSetupId)
    }
}

object WorldpayStubData extends CommonArbitraries {
  private var setupResponse: Option[WorldpayResponse] = None
  private var queryResponse: mutable.Map[String, WorldpayResponse] = mutable.Map.empty

  def recordSetupResponse(response: WorldpayResponse) =
    synchronized {
      setupResponse = Some(response)
    }

  def recordQueryResponse(transactionSetupId: String, response: WorldpayResponse) =
    synchronized {
      queryResponse += transactionSetupId -> response
    }

  def retrieveSetupResponse() =
    setupResponse.getOrElse(
      TransactionSetupResponse(
        transactionSetupId = UUID.randomUUID.toString,
      ),
    )

  def retrieveQueryResponse(transactionSetupId: String) =
    queryResponse.getOrElse(
      transactionSetupId,
      TransactionQueryResponse(
        accountId = "1066410",
        applicationId = "10025",
        approvalNumber = "651831",
        approvedAmount = 12.00,
        maskedCardNumber = "************0006",
        cardType = CardType.Visa,
        cardHolderName = "VANTIV TEST EMV 606- PayTouch",
        terminalId = "0060810007",
        transactionId = "19365463",
        transactionSetupId = transactionSetupId,
        hostResponseCode = "00",
      ),
    )
}
