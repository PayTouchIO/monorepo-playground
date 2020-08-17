package io.paytouch.core.resources.userroles

import java.util.UUID

import io.paytouch.core.data.model.{ Permission, PermissionUpdate, UserRoleRecord }
import io.paytouch.core.entities._
import io.paytouch.core.utils._

abstract class UserRolesFSpec extends FSpec {

  abstract class UserRoleResourceFSpecContext extends FSpecContext with DefaultFixtures {
    val userRoleDao = daos.userRoleDao

    def assertResponse(
        entity: UserRole,
        record: UserRoleRecord,
        withPermissions: Boolean,
        usersCount: Option[Int] = None,
      ) = {
      entity.id ==== record.id
      entity.name ==== record.name
      entity.hasDashboardAccess ==== record.hasDashboardAccess
      entity.hasRegisterAccess ==== record.hasRegisterAccess
      entity.hasTicketsAccess ==== record.hasTicketsAccess
      entity.dashboard ==== (if (withPermissions) Some(record.dashboard) else None)
      entity.register ==== (if (withPermissions) Some(record.register) else None)
      entity.usersCount ==== usersCount
      entity.createdAt ==== record.createdAt
      entity.updatedAt ==== record.updatedAt
    }

    def assertCreation(userRoleId: UUID, creation: UserRoleCreation) =
      assertUpdate(userRoleId, creation.asUpdate)

    def assertUpdate(userRoleId: UUID, update: UserRoleUpdate) = {
      val userRole = userRoleDao.findById(userRoleId).await.get
      if (update.name.isDefined) update.name ==== Some(userRole.name)
      if (update.hasDashboardAccess.isDefined) update.hasDashboardAccess ==== Some(userRole.hasDashboardAccess)
      if (update.hasRegisterAccess.isDefined) update.hasRegisterAccess ==== Some(userRole.hasRegisterAccess)
      if (update.hasTicketsAccess.isDefined) update.hasTicketsAccess ==== Some(userRole.hasTicketsAccess)
      if (update.dashboard.isDefined) assertPermissions(userRole.dashboard, update.dashboard.get)
      if (update.register.isDefined) assertPermissions(userRole.register, update.register.get)
    }

    def assertPermissions(entity: Permissions, update: PermissionsUpdate) = {
      if (update.categories.isDefined) assertPermission(entity.categories, update.categories.get)
      if (update.customers.isDefined) assertPermission(entity.customers, update.customers.get)
      if (update.discounts.isDefined) assertPermission(entity.discounts, update.discounts.get)
      if (update.employeeGroups.isDefined) assertPermission(entity.employeeGroups, update.employeeGroups.get)
      if (update.employeePayroll.isDefined) assertPermission(entity.employeePayroll, update.employeePayroll.get)
      if (update.employees.isDefined) assertPermission(entity.employees, update.employees.get)
      if (update.employeeTimeClock.isDefined) assertPermission(entity.employeeTimeClock, update.employeeTimeClock.get)
      if (update.inventory.isDefined) assertPermission(entity.inventory, update.inventory.get)
      if (update.invoices.isDefined) assertPermission(entity.invoices, update.invoices.get)
      if (update.locationSettings.isDefined) assertPermission(entity.locationSettings, update.locationSettings.get)
      if (update.marketing.isDefined) assertPermission(entity.marketing, update.marketing.get)
      if (update.modifiers.isDefined) assertPermission(entity.modifiers, update.modifiers.get)
      if (update.orders.isDefined) assertPermission(entity.orders, update.orders.get)
      if (update.products.isDefined) assertPermission(entity.products, update.products.get)
      if (update.purchaseOrders.isDefined) assertPermission(entity.purchaseOrders, update.purchaseOrders.get)
      if (update.reports.isDefined) assertPermission(entity.reports, update.reports.get)
      if (update.stockControl.isDefined) assertPermission(entity.stockControl, update.stockControl.get)
      if (update.suppliers.isDefined) assertPermission(entity.suppliers, update.suppliers.get)
      if (update.tips.isDefined) assertPermission(entity.tips, update.tips.get)
    }

    def assertPermission(entity: Permission, update: PermissionUpdate) = {
      if (update.read.isDefined) update.read ==== Some(entity.read)
      if (update.create.isDefined) update.create ==== Some(entity.create)
      if (update.edit.isDefined) update.edit ==== Some(entity.edit)
      if (update.delete.isDefined) update.delete ==== Some(entity.delete)
    }
  }
}
