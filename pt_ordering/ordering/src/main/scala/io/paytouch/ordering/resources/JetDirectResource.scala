package io.paytouch.ordering.resources

import scala.jdk.CollectionConverters._

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import cats.data.Validated.{ Invalid, Valid }

import com.typesafe.scalalogging.LazyLogging

import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair

import io.paytouch.ordering.entities.enums.PaymentProcessorCallbackStatus
import io.paytouch.ordering.entities.jetdirect.JetdirectCallbackStatus
import io.paytouch.ordering.resources.features.JsonResource
import io.paytouch.ordering.services.{ AuthenticationService, JetdirectService }

class JetdirectResource(val authenticationService: AuthenticationService, val jetDirectService: JetdirectService)
    extends JsonResource
       with LazyLogging {

  lazy val routes: Route =
    path("vendor" / "jetdirect" / "callbacks" / "receive") {
      post {
        formFieldMap { fields =>
          logger.debug(
            "[JetDirect] Received callback: {}",
            URLEncodedUtils.format(
              fields
                .map(pair => new BasicNameValuePair(pair._1, pair._2))
                .toList
                .asJava,
              "utf8",
            ),
          )
          val status = fields
            .get("responseText")
            .flatMap(JetdirectCallbackStatus.withNameInsensitiveOption(_).map(_.genericStatus))
            .getOrElse(PaymentProcessorCallbackStatus.Failure)
          onSuccess(jetDirectService.processCallback(fields, status)) {
            case Valid(_) => complete(StatusCodes.OK, None)
            case Invalid(i) =>
              logger.error(s"While processing jetDirect callback: $i. {status -> $status; fields -> $fields}")
              respondWithHeader(ValidationHeader(i)) {
                complete(StatusCodes.OK, None)
              }
          }
        }
      }
    }
}
