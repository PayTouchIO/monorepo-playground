package io.paytouch.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.model.{ HttpEntity, HttpRequest, HttpResponse, Uri }
import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, Sink, Source }
import io.paytouch.json.BaseJsonSupport
import io.paytouch.logging.{ BaseHttpLogging, LoggedRequest }

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.higherKinds

trait HttpClient extends StrictEntitiesHelper with BaseHttpLogging { self: BaseJsonSupport =>

  type Wrapper[T]

  protected def uri: Uri

  implicit def system: ActorSystem
  implicit val materializer: Materializer

  implicit val ec: ExecutionContext = system.dispatcher

  protected val httpClient: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]]

  protected def prepareRequest(request: HttpRequest): HttpRequest

  protected def sendRequest[T](request: HttpRequest, f: HttpResponse => Future[T]): Future[T] =
    Source
      .single(request)
      .via(httpClient)
      .mapAsync(1)(f)
      .runWith(Sink.head)

  protected def sendAndReceiveEmpty[Unit](
      _request: HttpRequest,
      successHandler: (HttpRequest, HttpEntity.Strict, HttpResponse, HttpEntity.Strict) => Future[Wrapper[Unit]],
    )(implicit
      mf: Manifest[Unit],
    ): Future[Wrapper[Unit]] =
    sendAndReceive[Unit](_request, successHandler, safelyParseFailure[Unit] _)
  protected def sendAndReceive[T](_request: HttpRequest)(implicit mf: Manifest[T]): Future[Wrapper[T]] =
    sendAndReceive[T](_request, safelyParseSuccess[T] _, safelyParseFailure[T] _)

  protected def sendAndReceive[T](
      _request: HttpRequest,
      successHandler: (HttpRequest, HttpEntity.Strict, HttpResponse, HttpEntity.Strict) => Future[Wrapper[T]],
      failureHandler: (HttpRequest, HttpEntity.Strict, HttpResponse, HttpEntity.Strict) => Future[Wrapper[T]],
    ): Future[Wrapper[T]] = {
    val request = prepareRequest(_request)
    val loggedRequest = LoggedRequest(request)
    val requestWithCompleteUri = prepareRequestWithCompleteUri(request)

    logRequestHeaders(loggedRequest)

    sendRequest(
      request,
      response =>
        processResponse(request, loggedRequest, response).flatMap {
          case (success, strictRequestEntity, strictResponseEntity) =>
            if (success)
              successHandler(requestWithCompleteUri, strictRequestEntity, response, strictResponseEntity)
            else
              failureHandler(requestWithCompleteUri, strictRequestEntity, response, strictResponseEntity)
        },
    )
  }

  private def processResponse(
      request: HttpRequest,
      loggedRequest: LoggedRequest,
      response: HttpResponse,
    ): Future[(Boolean, HttpEntity.Strict, HttpEntity.Strict)] =
    for {
      strictRequestEntity <- extractStrictEntity(request.entity)
      _ = logRequestBody(loggedRequest, strictRequestEntity)
      strictResponseEntity <- extractStrictEntity(response.entity)
      _ = logResponse(loggedRequest, response, strictResponseEntity)
      success <- isSuccess(response, strictResponseEntity)
    } yield (success, strictRequestEntity, strictResponseEntity)

  protected def prepareRequestWithCompleteUri(request: HttpRequest): HttpRequest = {
    val completeUri = request.uri.copy(scheme = uri.scheme, authority = uri.authority)
    request.copy(uri = completeUri)
  }

  protected def isSuccess(response: HttpResponse, responseEntity: HttpEntity.Strict): Future[Boolean] =
    Future.successful(response.status.isSuccess)

  protected def safelyParseSuccess[T](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    )(implicit
      mf: Manifest[T],
    ): Future[Wrapper[T]]

  protected def safelyParseFailure[T](
      request: HttpRequest,
      requestEntity: HttpEntity.Strict,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    )(implicit
      mf: Manifest[T],
    ): Future[Wrapper[T]]

}
