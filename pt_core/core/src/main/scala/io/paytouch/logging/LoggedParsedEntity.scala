package io.paytouch.logging

case class LoggedParsedEntity(
    `type`: String,
    entity: CleanEntity,
    loggedRequest: LoggedRequest,
    loggedResponse: Option[LoggedResponse],
  ) {
  lazy val message = s"Parsed ${`type`} entity"
}
