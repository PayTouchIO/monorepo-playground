package io.paytouch.ordering.resources

import akka.http.scaladsl.model.{ HttpHeader, StatusCodes }
import akka.http.scaladsl.server.Route

import cats.data._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.ordering.entities.stripe.StripeWebhook
import io.paytouch.ordering.LocationHeaderName
import io.paytouch.ordering.resources.features.StandardStoreResource
import io.paytouch.ordering.services.{ AuthenticationService, StripeService }

import cats.data.Validated.Valid
import akka.http.scaladsl.server.Directive1
import scala.concurrent.duration._

class StripeResource(val authenticationService: AuthenticationService, val stripeService: StripeService)
    extends StandardStoreResource
       with LazyLogging {

  // Required by the trait, but not used
  val resourcePath = "not_used"
  val paramName = "not_used"

  lazy val routes: Route =
    path("vendor" / "stripe" / "webhook") {
      post {
        headerValue(stripeSignature) { signature =>
          extractBody { payload =>
            entity(as[StripeWebhook]) { webhook =>
              onSuccess(stripeService.processWebhook(signature, payload, webhook)) {
                case Validated.Valid(_) =>
                  complete(StatusCodes.Accepted)

                case i @ Validated.Invalid(_) =>
                  logger.error(
                    s"While processing Stripe webhook: $i. {id -> ${webhook.id}}",
                  )
                  completeAsEmptyResponse(i)
              }
            }
          }
        }
      }
    }

  val stripeSignatureHeaderName = "Stripe-Signature".toLowerCase

  private def stripeSignature: HttpHeader => Option[String] = {
    case HttpHeader(`stripeSignatureHeaderName`, value) => Some(value)
    case _                                              => None
  }

  private def extractBody: Directive1[String] =
    extractStrictEntity(10.seconds).flatMap(entity => provide(entity.data.utf8String))
}
