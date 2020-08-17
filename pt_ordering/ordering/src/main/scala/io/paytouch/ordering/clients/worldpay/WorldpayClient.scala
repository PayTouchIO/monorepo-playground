package io.paytouch.ordering.clients.worldpay

import java.util.UUID

import akka.http.scaladsl.model.{ Uri => AkkaUri }

import com.softwaremill.sttp._
import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.ServiceConfigurations
import io.paytouch.ordering.clients.paytouch.core.entities.Location
import io.paytouch.ordering.data.model.{ CartRecord, WorldpayConfig }
import io.paytouch.ordering.clients.worldpay.entities._
import io.paytouch.ordering.withTag

import scala.concurrent.{ ExecutionContext, Future }
import scala.xml.{ Node, Utility, XML }

sealed trait WorldpayTransactionEndpointUri
sealed trait WorldpayReportingEndpointUri
sealed trait WorldpayCheckoutUri
sealed trait WorldpayReturnUri
sealed trait WorldpayApplicationId

class WorldpayClient(
    val transactionEndpointUri: AkkaUri withTag WorldpayTransactionEndpointUri,
    val reportingEndpointUri: AkkaUri withTag WorldpayReportingEndpointUri,
    val applicationId: String withTag WorldpayApplicationId,
    val returnUri: AkkaUri withTag WorldpayReturnUri,
  )(implicit
    ec: ExecutionContext,
    sttpBackend: SttpBackend[Id, Nothing],
  ) extends WorldpayParser
       with LazyLogging {

  def transactionSetup(
      config: WorldpayConfig,
      orderId: UUID,
      total: MonetaryAmount,
      storeName: Option[String],
    ): Future[WorldpayResponse] =
    call(transactionEndpointUri, transactionSetupBody(config, orderId, total, storeName))

  def transactionQuery(config: WorldpayConfig, transactionSetupId: String): Future[WorldpayResponse] =
    call(reportingEndpointUri, transactionQueryBody(config, transactionSetupId))

  private def call(uri: AkkaUri, requestBody: String): Future[WorldpayResponse] =
    Future {
      logger.info(s"Worldpay request uri=$uri body=$requestBody")

      val response = sttp
        .header("Content-Type", "text/xml")
        .body(requestBody)
        .post(uri"${uri.toString}")
        .mapResponse(parseResponse)
        .send()

      response.body match {
        case Right(r: WorldpayResponse) => r
        case _ =>
          logger.error(s"Unhandled worldpay response response=$response")
          ErrorResponse(ResponseCode.UnknownError)
      }
    }

  private def transactionSetupBody(
      config: WorldpayConfig,
      orderId: UUID,
      total: MonetaryAmount,
      storeName: Option[String],
    ): String = {
    s"""<TransactionSetup xmlns="https://transaction.elementexpress.com">
         <Credentials>
           <AccountID>${config.accountId}</AccountID>
           <AccountToken>${config.accountToken}</AccountToken>
           <AcceptorID>${config.acceptorId}</AcceptorID>
         </Credentials>
         <Application>
           <ApplicationID>${applicationId}</ApplicationID>
           <ApplicationName>Paytouch</ApplicationName>
           <ApplicationVersion>1.0</ApplicationVersion>
         </Application>
         <Transaction>
           <TransactionAmount>${total.roundedAmount}</TransactionAmount>
           <MarketCode>3</MarketCode>
           <TicketNumber>${orderId.toString}</TicketNumber>
           <ReferenceNumber>${orderId.toString}</ReferenceNumber>
         </Transaction>
         <Terminal>
           <TerminalID>${config.terminalId}</TerminalID>
           <TerminalCapabilityCode>5</TerminalCapabilityCode>
           <TerminalEnvironmentCode>6</TerminalEnvironmentCode>
           <CardPresentCode>3</CardPresentCode>
           <CVVPresenceCode>0</CVVPresenceCode>
           <CardInputCode>4</CardInputCode>
           <CardholderPresentCode>7</CardholderPresentCode>
           <MotoECICode>7</MotoECICode>
           <TerminalType>2</TerminalType>
         </Terminal>
         <TransactionSetup>
           <TransactionSetupMethod>1</TransactionSetupMethod>
           <AutoReturn>1</AutoReturn>
           <ReturnURL>${returnUri}</ReturnURL>
           <Embedded>0</Embedded>
           <CVVRequired>1</CVVRequired>
           <ProcessTransactionTitle>PAY ${total.show}</ProcessTransactionTitle>
           <CompanyName>${Utility.escape(storeName.getOrElse(""))}</CompanyName>
           <CustomCss>
             #body {
               font-family: helvetica;
               font-size: 15px;
               color: #000;
               background-image: linear-gradient(to bottom, #1585C5, #0b53b1);
               background-image: -webkit-linear-gradient(to bottom, #1585C5, #0b53b1);
               background-repeat: no-repeat;
               background-color: #0b53b1;
             }

             #tdTagline {
               display: none;
             }

             #tdWelcomeMessage {
               display: none;
             }

             #tdTagLineImage {
               display: none;
             }

             #tdLearnMore {
               padding-bottom: 30px;
               color: #fff;
             }

             #trFooter {
               display:none;
             }

             #imgClientLogo {
               border-radius: 4px;
             }

             #divHPForm {
               margin-top: 100px;
               max-width: 480px;
             }

             .tableStandard {
               border-spacing: 1px;
             }

             #tableTransactionEntry {
               background-color: #fff;
               border-radius: 4px;
               box-shadow: 0 0 20px rgba(0, 0, 0, 0.3);
               padding: 20px;
             }

             .tdHeader {
               border: none;
               background-color: #fff;
               font-size: 24px;
               font-weight: normal;
             }

             #divRequiredLegend {
               font-size: 12px;
             }

             #tableCardInformation {
               padding: 25px 50px;
             }

             #tdManualEntry {
               padding: 0 50px 25px;
             }

             .content {
               border: none;
               padding-top: 20px;
               padding-bottom: 20px;
               padding-left: 0px;
               padding-right: 0px;
             }

             #trTransactionInformation {
               display: none;
             }

             #tableTransactionButtons {
               margin-top: 0;
               bottom: 0;
             }

             #tdTransactionButtons {
               border: none;
             }

             #submit {
               background-color: #3EC289;
               border: none;
               padding: 10px;
               border-radius: 4px;
               font-size: 18px !important;
             }

             #btnCancel {
               color: #000;
             }

             #trManualEntryCardNumber {
               height: 50px;
             }

             #cardNumber {
               line-height: 35px;
               font-size: 18px;
               border-radius: 4px;
             }

             #ddlExpirationMonth {
               border-radius: 4px;
             }

             #ddlExpirationYear {
               border-radius: 4px;
             }

             #trManualEntryExpDate {
               height: 50px;
             }

             #trCVV {
               height: 50px;
             }

             #CVV {
               line-height: 35px;
               font-size: 18px;
               width: 60px;
               border-radius: 4px;
             }

             .hoverHelpText {
               color: #666;
               cursor: pointer;
             }

             .selectOption {
               font-size: 18px;
               border-color: #DDD;
             }

             .tdLabel {
               text-transform: uppercase;
               font-weight: normal;
               color: #666;
               text-align: left;
               padding-right: 30px;
             }

             .inputText {
               border-width: 1px;
               border-style: solid;
               border-color: #DDD;
               text-indent: 10px;
             }
           </CustomCss>
         </TransactionSetup>
       </TransactionSetup>"""
  }

  private def transactionQueryBody(config: WorldpayConfig, transactionSetupId: String): String =
    s"""<TransactionQuery xmlns="https://reporting.elementexpress.com">
         <Credentials>
           <AccountID>${config.accountId}</AccountID>
           <AccountToken>${config.accountToken}</AccountToken>
           <AcceptorID>${config.acceptorId}</AcceptorID>
         </Credentials>
         <Application>
           <ApplicationID>${applicationId}</ApplicationID>
           <ApplicationName>Paytouch</ApplicationName>
           <ApplicationVersion>1.0</ApplicationVersion>
         </Application>
         <Parameters>
           <TransactionSetupID>${transactionSetupId}</TransactionSetupID>
         </Parameters>
       </TransactionQuery>"""

  private def parseResponse(body: String): WorldpayResponse = {
    logger.info(s"Worldpay response body=${body}")

    val doc = XML.loadString(body)
    (doc \\ "ExpressResponseCode").head.text.toInt match {
      case 0 =>
        doc.label match {
          case "TransactionSetupResponse" =>
            val elem = doc \\ "TransactionSetup"
            val transactionSetupId: String = (elem \ "TransactionSetupID").text

            if (transactionSetupId.isEmpty) {
              logger.error(s"Invalid worldpay TransactionSetup response body=$body")
              ErrorResponse(ResponseCode.UnknownError)
            }
            else TransactionSetupResponse(transactionSetupId)

          case "TransactionQueryResponse" =>
            val maybeElem = (doc \\ "ReportingData" \\ "Item").find(e => (e \ "TransactionStatusCode").text.toInt == 1)
            maybeElem match {
              case None =>
                logger.error(
                  s"Unexpected worldpay TransactionQueryResponse. Expected an item with TransactionStatusCode = 0 (Approved). body=$body",
                )
                ErrorResponse(ResponseCode.UnknownError)

              case Some(elem) => parseTransactionQueryResponse(elem)
            }

          case _ =>
            logger.error(s"Unhandled worldpay response body=$body")
            ErrorResponse(ResponseCode.UnknownError)
        }
      case 90 =>
        logger.error(s"Worldpay no record response body=$body")
        ErrorResponse(ResponseCode.NoRecord)

      case 103 =>
        logger.error(s"Worldpay invalid request response body=$body")
        ErrorResponse(ResponseCode.InvalidRequest)

      case code =>
        logger.error(s"Unhandled worldpay response code code=$code body=$body")
        ErrorResponse(ResponseCode.UnknownError)
    }
  }
}
