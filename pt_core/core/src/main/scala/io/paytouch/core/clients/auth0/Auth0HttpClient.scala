package io.paytouch.core.clients.auth0

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
import java.net.URI

trait Auth0HttpClient extends HttpClient with JsonSupport with LazyLogging with HttpLogging {
  final override type Wrapper[T] = Either[Auth0ClientError, T]
  final protected type EitherAuth0ClientErrorOr[T] = Wrapper[T]

  protected def issuer: String

  protected val host = new URI(issuer).getHost
  protected val uri = Uri(host)
  protected val httpClient = Http().outgoingConnectionHttps(host = host)

  protected def prepareRequest(request: HttpRequest): HttpRequest = request

  protected def safelyParseSuccess[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): Future[Wrapper[T]] =
    jsonUnmarshaller[T].apply(responseEntity.withContentType(MediaTypes.`application/json`)).map(Right(_)).recoverWith {
      case ex: Throwable =>
        logger.error(s"While parsing response from Auth0 response = $response", ex)
        Future.successful(Left(Auth0ClientError(ex = Some(ex))))
    }

  protected def safelyParseFailure[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ) =
    jsonUnmarshaller[Auth0ClientError].apply(responseEntity).map(Left(_))

  implicit def toRichHttpRequest(req: HttpRequest): RichHttpRequest = new RichHttpRequest(req)

  class RichHttpRequest(val req: HttpRequest) {
    def withToken(token: String) = req.addHeader(Authorization(OAuth2BearerToken(token)))
  }
}
