package io.paytouch.logging

import akka.actor.{ Actor, DiagnosticActorLogging }
import akka.event.Logging
import akka.event.Logging.{ emptyMDC, LogLevel, MDC }
import akka.http.scaladsl.model._

import scala.concurrent.ExecutionContextExecutor

abstract class BaseMdcActor extends Actor with DiagnosticActorLogging {

  implicit def executionContext: ExecutionContextExecutor

  protected val `N/A` = "N/A"

  def receive = {
    case request: LoggedRequest   => log.info(request.message)
    case response: LoggedResponse => log.log(detectLogLevel(response), response.message)
    case requestEntity: LoggedRequestEntity =>
      self ! LoggedParsedEntity("request body", requestEntity.cleanEntity, requestEntity.loggedRequest, None)
    case responseEntity: LoggedResponseEntity =>
      self ! LoggedParsedEntity(
        "response body",
        responseEntity.cleanEntity,
        responseEntity.loggedResponse.loggedRequest,
        Some(responseEntity.loggedResponse),
      )
    case parsedEntity: LoggedParsedEntity => log.info(parsedEntity.message)
    case exception: LoggedRequestException =>
      val error =
        s"[${exception.request.id}] Something went wrong while converting body: ${exception.throwable}. exception_trace=${Logging
          .stackTraceFor(exception.throwable)} current_trace=${Logging.stackTraceFor(new Throwable())}"
      log.error(error)
    case exception: LoggedException =>
      log.error(exception.throwable, exception.throwable.getMessage)
  }

  private def detectLogLevel(response: LoggedResponse): LogLevel = {
    import StatusCodes._
    val acceptableFailures = Seq(NotFound, Unauthorized, Forbidden)
    val status = response.status
    if (status.isSuccess || acceptableFailures.contains(status)) Logging.InfoLevel
    else Logging.ErrorLevel
  }

  override def mdc(currentMessage: Any): MDC =
    currentMessage match {
      case request: LoggedRequest => typeMap("request") ++ requestMap(request)
      case response: LoggedResponse =>
        typeMap("response") ++ requestMap(response.loggedRequest) ++ responseMap(response)
      case parsedEntity: LoggedParsedEntity =>
        Map("data" -> parsedEntity.entity.body) ++ typeMap(parsedEntity.`type`) ++ requestMap(
          parsedEntity.loggedRequest,
        ) ++ parsedEntity
          .loggedResponse
          .fold(emptyMDC)(responseMap)
      case exception: LoggedRequestException => typeMap("exception") ++ requestMap(exception.request)
      case exception: LoggedException        => typeMap("exception") ++ requestMap(exception.request)
      case _                                 => Map.empty
    }

  private def typeMap(name: String): MDC = Map("type" -> name)

  protected def requestMap(loggedRequest: LoggedRequest): MDC =
    Map(
      "request_id" -> loggedRequest.id,
      "request_time" -> loggedRequest.time.toString,
      "method" -> loggedRequest.method.value,
      "uri" -> loggedRequest.uri.toString,
      "request_headers" -> toPrettyHeaders(loggedRequest.headers),
    )

  private def requestMap(request: HttpRequest): MDC =
    Map(
      "method" -> request.method.value,
      "uri" -> request.uri.toString,
      "request_headers" -> toPrettyHeaders(request.headers),
    )

  private def responseMap(loggedResponse: LoggedResponse): MDC =
    Map(
      "response_status" -> loggedResponse.status.intValue,
      "response_headers" -> toPrettyHeaders(loggedResponse.headers),
      "response_time" -> loggedResponse.time.toString,
      "duration" -> s"${loggedResponse.durationInMillis} ms",
    )

  private def toPrettyHeaders(headers: Seq[HttpHeader]): String =
    headers
      .filterNot(h => h.is("authorization") || h.is("timeout-access"))
      .map(h => s"${h.name}: ${h.value}")
      .mkString("[", ", ", "]")
}
