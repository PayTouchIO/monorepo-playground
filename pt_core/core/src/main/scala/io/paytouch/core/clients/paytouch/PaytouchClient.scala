package io.paytouch.core.clients.paytouch

import scala.concurrent._

import cats.implicits._

import akka.actor.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._

import io.paytouch.core._
import io.paytouch.core.entities.ApiResponse
import io.paytouch.core.logging._
import io.paytouch.logging.BaseMdcActor
import io.paytouch.utils.HttpClient

trait PaytouchClient extends HttpClient with HttpLogging {
  type Wrapper[T] = PaytouchResponse[T]

  protected def uri: Uri

  implicit def mdcActor: ActorRef withTag BaseMdcActor

  protected val httpClient = {
    val scheme = uri.scheme.toLowerCase
    val host = uri.authority.host.toString
    val port = uri.effectivePort
    if (scheme == "https") Http().outgoingConnectionHttps(host = host, port = port)
    else Http().outgoingConnection(host = host, port = port)
  }

  private val appName = RawHeader(ServiceConfigurations.AppHeaderName, "pt_ordering")
  private val versionApp = RawHeader(ServiceConfigurations.VersionHeaderName, "N/A")

  protected def prepareRequest(request: HttpRequest) = request.withDefaultHeaders(appName, versionApp)

  def sendAndReceiveAsApiResponse[T: Manifest](request: HttpRequest): Future[OrderingApiResponse[T]] =
    sendAndReceive[ApiResponse[T]](request)

  def sendAndReceiveEmpty(request: HttpRequest): Future[PaytouchResponse[Unit]] =
    sendAndReceive(request).map(_.void)

  protected def safelyParseSuccess[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): Future[PaytouchResponse[T]] =
    jsonUnmarshaller[T].apply(responseEntity).map(Right(_)).recover {
      case ex: Throwable => Left(errors.ClientError(uri, ex.getMessage))
    }

  protected def safelyParseFailure[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): Future[PaytouchResponse[T]] =
    jsonUnmarshaller[JValue].apply(responseEntity).map(err => Left(errors.ClientError(uri, err)))

  implicit def toRichHttpRequest(req: HttpRequest): RichHttpRequest = new RichHttpRequest(req)
  class RichHttpRequest(val req: HttpRequest) {

    def withAppAuth(implicit credentials: BasicHttpCredentials) = req.addHeader(Authorization(credentials))
  }

}
