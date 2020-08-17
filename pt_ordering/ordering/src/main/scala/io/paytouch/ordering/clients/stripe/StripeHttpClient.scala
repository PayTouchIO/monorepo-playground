package io.paytouch.ordering.clients.stripe

import scala.concurrent._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._

import com.typesafe.scalalogging.LazyLogging

import io.paytouch.implicits._

import io.paytouch.ordering.clients.stripe.entities._
import io.paytouch.ordering.data.model.StripeConfig
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.logging.HttpLogging
import io.paytouch.utils.HttpClient

trait StripeHttpClient extends HttpClient with JsonSupport with LazyLogging with HttpLogging {
  type Wrapper[T] = Either[StripeError, T]

  protected def config: StripeClientConfig
  protected def uri: Uri = config.baseUri.value

  protected val httpClient = {
    val scheme = uri.scheme.toLowerCase
    val host = uri.authority.host.toString
    val port = uri.effectivePort
    if (scheme == "https") Http().outgoingConnectionHttps(host = host, port = port)
    else Http().outgoingConnection(host = host, port = port)
  }

  val contentType: ContentType =
    ContentType
      .WithFixedCharset(MediaTypes.`application/x-www-form-urlencoded`)

  protected def prepareRequest(request: HttpRequest): HttpRequest =
    request
      .withDefaultHeaders(Authorization(BasicHttpCredentials(config.secretKey.value, "")))
      .withEntity(request.entity.withContentType(contentType))

  protected def safelyParseSuccess[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): Future[Wrapper[T]] =
    jsonUnmarshaller[T]
      .apply(responseEntity.withContentType(MediaTypes.`application/json`))
      .map(Right(_))
      .recoverWith {
        case ex: Throwable =>
          logger.error(s"While parsing response from Stripe response = $response", ex)
          Future.successful(Left(StripeError(ex = Some(ex))))
      }

  protected def safelyParseFailure[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): Future[Wrapper[T]] =
    jsonUnmarshaller[StripeErrorResponse]
      .apply(responseEntity)
      .map(errorResponse => Left(errorResponse.error))

  implicit def toRichHttpRequest(req: HttpRequest): RichHttpRequest = new RichHttpRequest(req)

  class RichHttpRequest(val req: HttpRequest) {
    def withCredentials(merchantConfig: StripeConfig) =
      req.addHeader(RawHeader("Stripe-Account", merchantConfig.accountId))
  }
}
