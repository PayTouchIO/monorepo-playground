package io.paytouch.core.clients.urbanairship

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.core.logging.HttpLogging
import io.paytouch.json.SnakeCamelCaseConversion
import io.paytouch.utils.HttpClient

import scala.concurrent._

trait UAClient extends HttpClient with LazyLogging with HttpLogging {

  type Wrapper[T] = UAResponse[T]

  def host: String
  def username: String
  def password: String

  implicit val s: SnakeCamelCaseConversion = SnakeCamelCaseConversion.False

  protected val httpClient = Http().outgoingConnectionHttps(host = host)
  protected val uri = Uri(host)

  private val authorization = Authorization(BasicHttpCredentials(username, password))
  private val apiRevision = RawHeader("Api-Revision", "1.2")

  // Let's revisit this and add scheme/protocol here so that it's visible in the logs.
  protected def prepareRequest(request: HttpRequest) =
    request.withDefaultHeaders(authorization, apiRevision)

  protected def safelyParseSuccess[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): Future[UAResponse[T]] =
    jsonUnmarshaller[T].apply(responseEntity.withContentType(MediaTypes.`application/json`)).map(Right(_)).recover {
      case ex: Throwable =>
        Error.asUAResponse(request, requestEntity, response, responseEntity, Some(ex))
    }

  protected def safelyParseFailure[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ) =
    Future.successful(Error.asUAResponse(request, requestEntity, response, responseEntity))

}
