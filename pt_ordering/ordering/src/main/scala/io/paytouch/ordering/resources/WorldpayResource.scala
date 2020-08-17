package io.paytouch.ordering.resources

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import cats.data._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.entities.enums.PaymentProcessorCallbackStatus
import io.paytouch.ordering.entities.worldpay._
import io.paytouch.ordering.LocationHeaderName
import io.paytouch.ordering.resources.features.StandardStoreResource
import io.paytouch.ordering.services.{ AuthenticationService, WorldpayService }
import cats.data.Validated.Valid

class WorldpayResource(val authenticationService: AuthenticationService, val worldpayService: WorldpayService)
    extends StandardStoreResource
       with LazyLogging {

  // Required by the trait, but not used
  val resourcePath = "not_used"
  val paramName = "not_used"

  lazy val routes: Route =
    path("vendor" / "worldpay" / "callbacks" / "receive") {
      get {
        parameters("TransactionSetupID", "HostedPaymentStatus") { (transactionSetupId, statusText) =>
          parameterMap { params =>
            val status = WorldpayPaymentStatus.withNameInsensitive(statusText)

            onSuccess(worldpayService.receive(transactionSetupId, status, params)) {
              case Validated.Valid(url) =>
                respondWithHeader(RawHeader(LocationHeaderName, url.toString)) {
                  complete(StatusCodes.Found, None)
                }

              case i @ Validated.Invalid(_) =>
                logger.error(
                  s"While processing Worldpay receive: $i. {transactionSetupId -> $transactionSetupId, status -> $status}",
                )

                completeAsEmptyResponse(i)
            }
          }
        }
      }
    }
}
