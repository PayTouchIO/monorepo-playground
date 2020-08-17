package io.paytouch.logging

import java.time.{ Duration, ZonedDateTime }

import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }

import scala.concurrent.Future

case class LoggedResponse private (
    loggedRequest: LoggedRequest,
    private val response: HttpResponse,
    time: ZonedDateTime,
  ) {
  lazy val status = response.status
  lazy val headers = response.headers
  lazy val message = s"${response.status}"
  val durationInMillis = Duration.between(loggedRequest.time, time).toMillis
}

object LoggedResponse {
  def apply(request: LoggedRequest, response: HttpResponse): LoggedResponse =
    apply(request, response, ZonedDateTime.now)
}

case class LoggedResponseEntity(loggedResponse: LoggedResponse, cleanEntity: CleanEntity)
