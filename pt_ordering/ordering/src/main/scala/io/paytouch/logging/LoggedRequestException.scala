package io.paytouch.logging

import java.time.ZonedDateTime

import akka.http.scaladsl.model.HttpRequest

case class LoggedRequestException(request: LoggedRequest, throwable: Throwable)
