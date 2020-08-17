package io.paytouch.logging

import akka.actor._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.RouteResult.{ Complete, Rejected }
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{ BasicDirectives, DebuggingDirectives, LoggingMagnet }

import io.paytouch.json.BaseJsonSupport
import io.paytouch.utils.Tagging._
import io.paytouch.utils.{ RejectionMsg, StrictEntitiesDirectories, StrictEntitiesHelper }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

trait LogPostResponse
trait ResponseTimeout

trait BaseHttpLogging extends StrictEntitiesDirectories with StrictEntitiesHelper { self: BaseJsonSupport =>

  def logPostResponse: Boolean withTag LogPostResponse
  def responseTimeout: FiniteDuration withTag ResponseTimeout

  implicit def system: ActorSystem
  implicit def ec: ExecutionContext

  implicit def mdcActor: ActorRef withTag BaseMdcActor

  private val PathsToIgnore = Seq("/ping")

  val customLogRequestResponse: Directive0 =
    BasicDirectives.extractRequestContext.flatMap { ctx =>
      val loggedRequest = LoggedRequest(ctx.request)
      DebuggingDirectives.logRequest(logRequestWithMcd(loggedRequest)) &
        BasicDirectives.extractStrictEntity(responseTimeout).flatMap { requestEntity =>
          DebuggingDirectives.logRequestResult(logRequestBodyAndResponseWithMcd(loggedRequest, requestEntity))
        }
    }

  private def logRequestWithMcd(loggedRequest: LoggedRequest): LoggingMagnet[HttpRequest => Unit] =
    LoggingMagnet { _ => _ =>
      if (requestShouldBeLogged(loggedRequest))
        logRequestHeaders(loggedRequest)
    }

  private def logRequestBodyAndResponseWithMcd(
      loggedRequest: LoggedRequest,
      requestBody: HttpEntity.Strict,
    ): LoggingMagnet[HttpRequest => RouteResult => Unit] =
    LoggingMagnet { _ => _ =>
      if (requestShouldBeLogged(loggedRequest)) {
        logRequestBody(loggedRequest, requestBody)
        routeResult => {
          val response = parseResponse(routeResult)
          extractStrictEntity(response.entity) onComplete {
            case Success(responseEntity) =>
              logResponse(loggedRequest, response, responseEntity)
            case Failure(ex) => mdcActor ! LoggedRequestException(loggedRequest, ex)
          }
        }
      }
      else
        _ => {}
    }

  protected def logRequestHeaders(loggedRequest: LoggedRequest): Unit =
    mdcActor ! loggedRequest

  protected def logResponse(
      loggedRequest: LoggedRequest,
      response: HttpResponse,
      responseEntity: HttpEntity.Strict,
    ): LoggedResponse = {
    val loggedResponse = LoggedResponse(loggedRequest, response)
    mdcActor ! loggedResponse
    logResponseBody(loggedResponse, responseEntity)
    loggedResponse
  }

  protected def logRequestBody(loggedRequest: LoggedRequest, requestEntity: HttpEntity.Strict) =
    if (requestBodyShouldBeLogged(loggedRequest)) {
      val cleanEntity = CleanEntity.apply(requestEntity)
      mdcActor ! LoggedRequestEntity(loggedRequest, cleanEntity)
    }

  private def logResponseBody(loggedResponse: LoggedResponse, responseEntity: HttpEntity.Strict) =
    if (responseBodyShouldBeLogged(loggedResponse)) {
      val cleanEntity = CleanEntity.apply(responseEntity)
      mdcActor ! LoggedResponseEntity(loggedResponse, cleanEntity)
    }

  private def requestShouldBeLogged(request: LoggedRequest): Boolean = {
    val pathToIgnore = PathsToIgnore.contains(request.uri.getPathString)
    val optionRequest = request.method == HttpMethods.OPTIONS
    !pathToIgnore && !optionRequest
  }

  private def requestBodyShouldBeLogged(loggedRequest: LoggedRequest) =
    Set(HttpMethods.POST, HttpMethods.PUT)(loggedRequest.method) &&
      loggedRequest.contentType == ContentTypes.`application/json`

  private def responseBodyShouldBeLogged(loggedResponse: LoggedResponse): Boolean = {
    val loggedRequest = loggedResponse.loggedRequest
    logResponseRequested(loggedResponse) || (requestBodyShouldBeLogged(loggedRequest) && !isAuthenticationEndpoint(
      loggedRequest,
    ))
  }

  private def logResponseRequested(response: LoggedResponse): Boolean =
    logPostResponse || response.status.isFailure

  private def isAuthenticationEndpoint(request: LoggedRequest): Boolean =
    request.uri.path.endsWith(".auth")

  private def parseResponse(result: RouteResult) =
    result match {
      case Complete(resp) => resp
      case Rejected(rejections) =>
        val status = inferStatusFromRejection(rejections)
        val rejectionsToLog = rejections.filterNot(_.isInstanceOf[TransformationRejection])
        val rejectionsMsg = RejectionMsg(rejectionsToLog.mkString(","))
        val entity = HttpEntity(fromEntityToString(rejectionsMsg))
        HttpResponse(status = status, entity = entity)
    }

  private def inferStatusFromRejection(rejections: Seq[Rejection]): StatusCode =
    rejections match {
      case rjs if rjs.isEmpty || rjs.exists(_.isInstanceOf[ValidationRejection]) => StatusCodes.NotFound
      case rjs if rjs.exists(_.isInstanceOf[AuthenticationFailedRejection])      => StatusCodes.Unauthorized
      case _                                                                     => StatusCodes.BadRequest
    }
}
