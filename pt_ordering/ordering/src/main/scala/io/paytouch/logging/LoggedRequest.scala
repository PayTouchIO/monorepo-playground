package io.paytouch.logging

import java.time.ZonedDateTime
import java.util.UUID

import akka.http.scaladsl.model._

case class LoggedRequest private (
    id: UUID,
    private val request: HttpRequest,
    time: ZonedDateTime,
  ) {
  lazy val uri = request.getUri
  lazy val method = request.method
  lazy val headers = request.headers
  lazy val contentType = request.entity.contentType

  lazy val message = s"${request.method.value} ${request.uri}"
}

object LoggedRequest {
  def apply(request: HttpRequest): LoggedRequest = LoggedRequest(UUID.randomUUID, request, ZonedDateTime.now)
}

case class LoggedRequestEntity(loggedRequest: LoggedRequest, cleanEntity: CleanEntity)
