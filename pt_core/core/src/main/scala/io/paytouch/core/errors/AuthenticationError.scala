package io.paytouch.core.errors

import java.util.UUID

import io.paytouch.core.entities.enums.LoginSource

final case class UnauthorizedError(
    message: String,
    values: Seq[UUID] = Seq.empty,
    objectId: Option[UUID] = None,
    field: Option[String] = None,
  ) extends Unauthorized

case object UserDisabled {
  def apply(): UnauthorizedError = UnauthorizedError(message = "The user is disabled")
}

case object UserDeleted {
  def apply(): UnauthorizedError = UnauthorizedError(message = "The user is deleted")
}

case object NoUserRoleAssociated {
  def apply(): UnauthorizedError =
    UnauthorizedError(message = "The user must be associated to a user role")
}

case object UserRoleNotFound {
  def apply(id: UUID): UnauthorizedError =
    UnauthorizedError(message = s"User role $id not found", objectId = Some(id))
}

case object InaccessibleSource {
  def apply(source: LoginSource): UnauthorizedError =
    UnauthorizedError(message = s"User does not have access to source $source")
}
