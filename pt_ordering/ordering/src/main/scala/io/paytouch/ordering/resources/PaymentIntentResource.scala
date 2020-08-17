package io.paytouch.ordering.resources

import java.util.UUID

import akka.http.scaladsl.server.Route
import io.paytouch.ordering.UpsertionResult
import io.paytouch.ordering.entities.{ PaymentIntent, PaymentIntentCreation }
import io.paytouch.ordering.resources.features.StandardRapidoResource
import io.paytouch.ordering.services.{ AuthenticationService, PaymentIntentService }
import io.paytouch.ordering.utils.validation.ValidatedData.ValidatedData

import scala.concurrent.Future

class PaymentIntentResource(
    val authenticationService: AuthenticationService,
    val paymentIntentService: PaymentIntentService,
  ) extends StandardRapidoResource {

  val resourcePath = "payment_intents"
  val paramName = "payment_intent_id"

  lazy val routes: Route =
    createPaymentIntent(implicit context => paymentIntentService.create)

  private def createPaymentIntent(
      f: Context => (UUID, PaymentIntentCreation) => Future[UpsertionResult[PaymentIntent]],
    ) =
    (path(s"$resourcePath.create") & post) {
      parameters(s"$paramName".as[UUID]) { id =>
        entity(as[PaymentIntentCreation]) { creation =>
          contextAuthenticationFromRapidoOrder(creation.merchantId, creation.orderId) { context =>
            onSuccess(f(context)(id, creation))(result => completeAsApiResponse(result))
          }
        }
      }
    }
}
