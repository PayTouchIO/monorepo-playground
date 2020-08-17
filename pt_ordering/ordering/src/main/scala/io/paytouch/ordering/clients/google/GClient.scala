package io.paytouch.ordering.clients.google

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import com.typesafe.scalalogging.LazyLogging
import io.paytouch.ordering.clients.google.entities.{ GError, GResponseStatus, GStatus }
import io.paytouch.ordering.json.JsonSupport
import io.paytouch.ordering.logging.HttpLogging
import io.paytouch.utils.HttpClient

import scala.concurrent.Future

trait GClient extends HttpClient with JsonSupport with LazyLogging with HttpLogging {

  type Wrapper[T] = Either[GError, T]

  protected def referer: Uri
  protected def uri: Uri
  def key: String

  protected val httpClient = {
    val scheme = uri.scheme.toLowerCase
    val host = uri.authority.host.toString
    val port = uri.effectivePort
    if (scheme == "https") Http().outgoingConnectionHttps(host = host, port = port)
    else Http().outgoingConnection(host = host, port = port)
  }

  private val referHeader = Referer(referer)

  protected def prepareRequest(request: HttpRequest): HttpRequest = request.withDefaultHeaders(referHeader)

  protected def safelyParseSuccess[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): Future[Wrapper[T]] =
    jsonUnmarshaller[T].apply(responseEntity.withContentType(MediaTypes.`application/json`)).map(Right(_)).recoverWith {
      case ex: Throwable =>
        logger.error("While parsing response from Google", ex)
        Future.successful(Left(GError(ex = Some(ex))))
    }

  protected def safelyParseFailure[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ) =
    jsonUnmarshaller[GError].apply(responseEntity).map(Left(_))

  override protected def isSuccess(response: HttpResponse, responseEntity: HttpEntity.Strict): Future[Boolean] =
    jsonUnmarshaller[GResponseStatus].apply(responseEntity).map { responseStatus =>
      responseStatus.status == GStatus.Ok
    }

  implicit def toRichHttpRequest(req: HttpRequest): RichHttpRequest = new RichHttpRequest(req)

  class RichHttpRequest(val req: HttpRequest) {

    def withAPIKey = {
      val queryWithKey = (req.uri.rawQueryString.toList ++ List(s"key=$key")).mkString("&")
      val uriWithKey = req.uri.withRawQueryString(queryWithKey)
      req.withUri(uriWithKey)
    }
  }
}
