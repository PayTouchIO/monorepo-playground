package io.paytouch.logging

case class LoggedParsedEntity(
    `type`: String,
    entity: CleanEntity,
    loggedRequest: LoggedRequest,
    loggedResponse: Option[LoggedResponse],
  )
