package io.paytouch.core.entities

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.enums.ExposedName

final case class UserRole(
    id: UUID,
    name: String,
    hasDashboardAccess: Boolean,
    hasOnlineStorefrontAccess: Boolean,
    hasRegisterAccess: Boolean,
    hasTicketsAccess: Boolean,
    dashboard: Option[Permissions],
    register: Option[Permissions],
    usersCount: Option[Int],
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends ExposedEntity {
  val classShortName = ExposedName.UserRole
}

final case class UserRoleCreation(
    name: String,
    hasDashboardAccess: Boolean,
    hasOnlineStorefrontAccess: Boolean,
    hasRegisterAccess: Boolean,
    hasTicketsAccess: Boolean,
    dashboard: PermissionsUpdate,
    register: PermissionsUpdate,
  ) extends CreationEntity[UserRole, UserRoleUpdate] {

  def asUpdate: UserRoleUpdate =
    UserRoleUpdate(
      name = Some(name),
      hasDashboardAccess = Some(hasDashboardAccess),
      hasOnlineStorefrontAccess = Some(hasOnlineStorefrontAccess),
      hasRegisterAccess = Some(hasRegisterAccess),
      hasTicketsAccess = Some(hasTicketsAccess),
      dashboard = Some(dashboard),
      register = Some(register),
    )
}

final case class UserRoleUpdate(
    name: Option[String],
    hasDashboardAccess: Option[Boolean],
    hasOnlineStorefrontAccess: Option[Boolean],
    hasRegisterAccess: Option[Boolean],
    hasTicketsAccess: Option[Boolean],
    dashboard: Option[PermissionsUpdate],
    register: Option[PermissionsUpdate],
  ) extends UpdateEntity[UserRole]
