package io.paytouch.core.data.tables

import java.time.ZonedDateTime
import java.util.UUID

import io.paytouch.core.data.driver.CustomPostgresDriver.api._
import io.paytouch.core.data.model.UserRoleRecord
import io.paytouch.core.entities.Permissions

class UserRolesTable(tag: Tag) extends SlickMerchantTable[UserRoleRecord](tag, "user_roles") {
  def id = column[UUID]("id", O.PrimaryKey)
  def merchantId = column[UUID]("merchant_id")

  def name = column[String]("name")
  def hasDashboardAccess = column[Boolean]("has_dashboard_access")
  def hasOnlineStorefrontAccess = column[Boolean]("has_online_storefront_access")
  def hasRegisterAccess = column[Boolean]("has_register_access")
  def hasTicketsAccess = column[Boolean]("has_tickets_access")
  def dashboard = column[Permissions]("dashboard")
  def register = column[Permissions]("register")

  def createdAt = column[ZonedDateTime]("created_at")
  def updatedAt = column[ZonedDateTime]("updated_at")

  def * =
    (
      id,
      merchantId,
      name,
      hasDashboardAccess,
      hasOnlineStorefrontAccess,
      hasRegisterAccess,
      hasTicketsAccess,
      dashboard,
      register,
      createdAt,
      updatedAt,
    ).<>(UserRoleRecord.tupled, UserRoleRecord.unapply)
}
