package io.paytouch.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.model.{ HttpEntity, HttpRequest, HttpResponse, Uri }
import akka.stream.scaladsl.{ Flow, Sink, Source }

import io.paytouch.json.BaseJsonSupport
import io.paytouch.logging.{ BaseHttpLogging, LoggedRequest }

import scala.concurrent._

trait HttpClient extends StrictEntitiesHelper with BaseHttpLogging { self: BaseJsonSupport =>

  type Wrapper[T]

  protected def uri: Uri

  implicit def system: ActorSystem

  implicit val ec: ExecutionContext = system.dispatcher

  protected val httpClient: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]]

  protected def prepareRequest(request: HttpRequest): HttpRequest

  protected def sendAndReceive[T: Manifest](_request: HttpRequest): Future[Wrapper[T]] = {
    val request = prepareRequest(_request)
    val loggedRequest = LoggedRequest(request)
    logRequestHeaders(loggedRequest)

    Source
      .single(request)
      .via(httpClient)
      .mapAsync(1) { response =>
        for {
          strictRequestEntity <- extractStrictEntity(request.entity)
          _ = logRequestBody(loggedRequest, strictRequestEntity)
          result <- processResponse(loggedRequest, request, strictRequestEntity, response)
        } yield result
      }
      .runWith(Sink.head)
  }

  protected def processResponse[T: Manifest](
      loggedRequest: LoggedRequest,
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
    ): Future[Wrapper[T]] = {
    val completeUri = request.uri.copy(scheme = uri.scheme, authority = uri.authority)
    val requestWithCompleteUri = request.copy(uri = completeUri)

    extractStrictEntity(response.entity).flatMap { strictResponseEntity =>
      logResponse(loggedRequest, response, strictResponseEntity)
      isSuccess(response, strictResponseEntity).flatMap { success =>
        if (success)
          safelyParseSuccess[T](requestWithCompleteUri, requestEntity, response, strictResponseEntity)
        else
          safelyParseFailure[T](requestWithCompleteUri, requestEntity, response, strictResponseEntity)
      }
    }
  }

  protected def isSuccess(response: HttpResponse, responseEntity: HttpEntity.Strict): Future[Boolean] =
    Future.successful(response.status.isSuccess)

  protected def safelyParseSuccess[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): Future[Wrapper[T]]

  protected def safelyParseFailure[T: Manifest](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): Future[Wrapper[T]]

}
