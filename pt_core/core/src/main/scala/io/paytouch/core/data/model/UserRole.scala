package io.paytouch.core.data.model

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.entities.{ Permissions, PermissionsUpdate }

final case class UserRoleRecord(
    id: UUID,
    merchantId: UUID,
    name: String,
    hasDashboardAccess: Boolean,
    hasOnlineStorefrontAccess: Boolean,
    hasRegisterAccess: Boolean,
    hasTicketsAccess: Boolean,
    dashboard: Permissions,
    register: Permissions,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
  ) extends SlickMerchantRecord

case class UserRoleUpdate(
    id: Option[UUID],
    merchantId: Option[UUID],
    name: Option[String],
    hasDashboardAccess: Option[Boolean],
    hasOnlineStorefrontAccess: Option[Boolean],
    hasRegisterAccess: Option[Boolean],
    hasTicketsAccess: Option[Boolean],
    dashboard: Option[PermissionsUpdate],
    register: Option[PermissionsUpdate],
  ) extends SlickMerchantUpdate[UserRoleRecord] {

  def toRecord: UserRoleRecord = {
    require(merchantId.isDefined, s"Impossible to convert UserRoleUpdate without a merchant id. [$this]")
    require(name.isDefined, s"Impossible to convert UserRoleUpdate without a name. [$this]")
    UserRoleRecord(
      id = id.getOrElse(UUID.randomUUID),
      merchantId = merchantId.get,
      name = name.get,
      hasDashboardAccess = hasDashboardAccess.getOrElse(true),
      hasOnlineStorefrontAccess = hasOnlineStorefrontAccess.getOrElse(false),
      hasRegisterAccess = hasRegisterAccess.getOrElse(false),
      hasTicketsAccess = hasTicketsAccess.getOrElse(false),
      dashboard = dashboard.map(_.toRecord).getOrElse(Permissions()),
      register = register.map(_.toRecord).getOrElse(Permissions()),
      createdAt = now,
      updatedAt = now,
    )
  }

  def updateRecord(record: UserRoleRecord): UserRoleRecord =
    UserRoleRecord(
      id = id.getOrElse(record.id),
      merchantId = merchantId.getOrElse(record.merchantId),
      name = name.getOrElse(record.name),
      hasDashboardAccess = hasDashboardAccess.getOrElse(record.hasDashboardAccess),
      hasOnlineStorefrontAccess = hasOnlineStorefrontAccess.getOrElse(record.hasOnlineStorefrontAccess),
      hasRegisterAccess = hasRegisterAccess.getOrElse(record.hasRegisterAccess),
      hasTicketsAccess = hasTicketsAccess.getOrElse(record.hasTicketsAccess),
      dashboard = dashboard.map(_.updateRecord(record.dashboard)).getOrElse(record.dashboard),
      register = register.map(_.updateRecord(record.register)).getOrElse(record.register),
      createdAt = record.createdAt,
      updatedAt = now,
    )
}

object UserRoleUpdate {
  val Admin = "Admin"
  val Manager = "Manager"
  private val Employee = "Employee"
  private val Cashier = "Cashier"
  private val Temp = "Temp"

  def defaults(merchantId: UUID, setupType: enums.SetupType): Seq[UserRoleUpdate] =
    setupType match {
      case enums.SetupType.Dash =>
        Seq(
          Dash.manager(merchantId),
          Dash.employee(merchantId),
        )

      case enums.SetupType.Paytouch =>
        Seq(
          Paytouch.admin(merchantId),
          Paytouch.manager(merchantId),
          Paytouch.employee(merchantId),
          Paytouch.cashier(merchantId),
          Paytouch.temp(merchantId),
        )
    }

  object Dash {
    def manager(merchantId: UUID) =
      apply(
        merchantId,
        Manager,
        PermissionsUpdate.Dash.Manager,
        hasDashboardAccess = Some(true),
        hasOnlineStorefrontAccess = Some(true),
        hasTicketsAccess = Some(true),
        hasRegisterAccess = Some(true),
      )

    def employee(merchantId: UUID) =
      apply(
        merchantId,
        Employee,
        PermissionsUpdate.Dash.Employee,
        hasDashboardAccess = Some(false),
        hasOnlineStorefrontAccess = Some(false),
        hasTicketsAccess = Some(true),
        hasRegisterAccess = Some(true),
      )
  }

  object Paytouch {
    def admin(merchantId: UUID) =
      apply(
        merchantId,
        Admin,
        PermissionsUpdate.Paytouch.Admin,
        hasDashboardAccess = Some(true),
        hasOnlineStorefrontAccess = Some(true),
        hasTicketsAccess = Some(true),
        hasRegisterAccess = Some(true),
      )

    def manager(merchantId: UUID) =
      apply(
        merchantId,
        Manager,
        PermissionsUpdate.Paytouch.Manager,
        hasDashboardAccess = Some(true),
        hasOnlineStorefrontAccess = Some(true),
        hasTicketsAccess = Some(true),
        hasRegisterAccess = Some(true),
      )

    def employee(merchantId: UUID) =
      apply(
        merchantId,
        Employee,
        PermissionsUpdate.Paytouch.Employee,
        hasDashboardAccess = Some(true),
        hasOnlineStorefrontAccess = Some(false),
        hasTicketsAccess = Some(true),
        hasRegisterAccess = Some(false),
      )

    def cashier(merchantId: UUID) =
      apply(
        merchantId,
        Cashier,
        PermissionsUpdate.Paytouch.Cashier,
        hasDashboardAccess = Some(true),
        hasOnlineStorefrontAccess = Some(false),
        hasTicketsAccess = Some(true),
        hasRegisterAccess = Some(false),
      )

    def temp(merchantId: UUID) =
      apply(
        merchantId,
        Temp,
        PermissionsUpdate.Paytouch.Temp,
        hasDashboardAccess = Some(true),
        hasOnlineStorefrontAccess = Some(false),
        hasTicketsAccess = Some(false),
        hasRegisterAccess = Some(false),
      )
  }

  private def apply(
      merchantId: UUID,
      name: String,
      permissions: PermissionsUpdate,
      hasDashboardAccess: Option[Boolean],
      hasOnlineStorefrontAccess: Option[Boolean],
      hasTicketsAccess: Option[Boolean],
      hasRegisterAccess: Option[Boolean],
    ): UserRoleUpdate =
    UserRoleUpdate(
      id = None,
      merchantId = Some(merchantId),
      name = Some(name),
      hasDashboardAccess = hasDashboardAccess,
      hasOnlineStorefrontAccess = hasOnlineStorefrontAccess,
      hasTicketsAccess = hasTicketsAccess,
      hasRegisterAccess = hasRegisterAccess,
      dashboard = Some(permissions),
      register = Some(permissions),
    )
}
