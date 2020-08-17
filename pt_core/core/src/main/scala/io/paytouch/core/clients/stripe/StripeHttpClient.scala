package io.paytouch.core.clients.stripe

import scala.concurrent._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._

import cats.implicits._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.core.clients.stripe.entities._
import io.paytouch.core.data.model.PaymentProcessorConfig
import io.paytouch.core.json.JsonSupport
import io.paytouch.core.logging.HttpLogging
import io.paytouch.utils.HttpClient

trait StripeHttpClient extends HttpClient with JsonSupport with LazyLogging with HttpLogging {
  final override type Wrapper[T] = Either[StripeError, T]
  final protected type EitherStripeErrorOr[T] = Wrapper[T]

  protected def uri: Uri
  protected def secretKey: String

  protected val httpClient = {
    val scheme = uri.scheme.toLowerCase
    val host = uri.authority.host.toString
    val port = uri.effectivePort
    if (scheme === "https") Http().outgoingConnectionHttps(host = host, port = port)
    else Http().outgoingConnection(host = host, port = port)
  }

  private val authHeader = Authorization(BasicHttpCredentials(secretKey, ""))

  protected def prepareRequest(request: HttpRequest): HttpRequest = request.withDefaultHeaders(authHeader)

  protected def safelyParseSuccess[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): Future[Wrapper[T]] =
    jsonUnmarshaller[T].apply(responseEntity.withContentType(MediaTypes.`application/json`)).map(Right(_)).recoverWith {
      case ex: Throwable =>
        logger.error(s"While parsing response from Stripe response = $response", ex)
        Future.successful(Left(StripeError(ex = Some(ex))))
    }

  protected def safelyParseFailure[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ) =
    jsonUnmarshaller[StripeError].apply(responseEntity).map(Left(_))

  implicit def toRichHttpRequest(req: HttpRequest): RichHttpRequest = new RichHttpRequest(req)

  class RichHttpRequest(val req: HttpRequest) {
    def withCredentials(merchantConfig: PaymentProcessorConfig.Stripe) =
      req.addHeader(RawHeader("Stripe-Account", merchantConfig.accountId))
  }
}
