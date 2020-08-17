package io.paytouch.logging

import java.time.ZonedDateTime

import akka.http.scaladsl.model.HttpRequest

case class LoggedException(
    request: HttpRequest,
    throwable: Throwable,
    time: ZonedDateTime,
  )
