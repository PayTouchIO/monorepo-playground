package io.paytouch.core.entities

import io.paytouch.core.data.model.{ Permission, PermissionUpdate }

final case class Permissions(
    categories: Permission = Permission(),
    cashDrawers: Permission = Permission(),
    customers: Permission = Permission(),
    discounts: Permission = Permission(),
    employeeGroups: Permission = Permission(),
    employeePayroll: Permission = Permission(),
    employees: Permission = Permission(),
    employeeTimeClock: Permission = Permission(),
    inventory: Permission = Permission(),
    invoices: Permission = Permission(),
    locationSettings: Permission = Permission(),
    marketing: Permission = Permission(),
    modifiers: Permission = Permission(),
    orders: Permission = Permission(),
    products: Permission = Permission(),
    purchaseOrders: Permission = Permission(),
    reports: Permission = Permission(),
    stockControl: Permission = Permission(),
    suppliers: Permission = Permission(),
    refunds: Permission = Permission(),
    giftCards: Permission = Permission(),
    tips: Permission = Permission(),
  )

final case class PermissionsUpdate(
    categories: Option[PermissionUpdate],
    cashDrawers: Option[PermissionUpdate],
    customers: Option[PermissionUpdate],
    discounts: Option[PermissionUpdate],
    employeeGroups: Option[PermissionUpdate],
    employeePayroll: Option[PermissionUpdate],
    employees: Option[PermissionUpdate],
    employeeTimeClock: Option[PermissionUpdate],
    inventory: Option[PermissionUpdate],
    invoices: Option[PermissionUpdate],
    locationSettings: Option[PermissionUpdate],
    marketing: Option[PermissionUpdate],
    modifiers: Option[PermissionUpdate],
    orders: Option[PermissionUpdate],
    products: Option[PermissionUpdate],
    purchaseOrders: Option[PermissionUpdate],
    reports: Option[PermissionUpdate],
    stockControl: Option[PermissionUpdate],
    suppliers: Option[PermissionUpdate],
    refunds: Option[PermissionUpdate],
    giftCards: Option[PermissionUpdate],
    tips: Option[PermissionUpdate],
  ) {
  def toRecord: Permissions = updateRecord(Permissions())

  def updateRecord(record: Permissions): Permissions =
    Permissions(
      categories = categories.map(_.updateRecord(record.categories)).getOrElse(record.categories),
      cashDrawers = cashDrawers.map(_.updateRecord(record.cashDrawers)).getOrElse(record.cashDrawers),
      customers = customers.map(_.updateRecord(record.customers)).getOrElse(record.customers),
      discounts = discounts.map(_.updateRecord(record.discounts)).getOrElse(record.discounts),
      employeeGroups = employeeGroups.map(_.updateRecord(record.employeeGroups)).getOrElse(record.employeeGroups),
      employeePayroll = employeePayroll.map(_.updateRecord(record.employeePayroll)).getOrElse(record.employeePayroll),
      employees = employees.map(_.updateRecord(record.employees)).getOrElse(record.employees),
      employeeTimeClock =
        employeeTimeClock.map(_.updateRecord(record.employeeTimeClock)).getOrElse(record.employeeTimeClock),
      inventory = inventory.map(_.updateRecord(record.inventory)).getOrElse(record.inventory),
      invoices = invoices.map(_.updateRecord(record.invoices)).getOrElse(record.invoices),
      locationSettings =
        locationSettings.map(_.updateRecord(record.locationSettings)).getOrElse(record.locationSettings),
      marketing = marketing.map(_.updateRecord(record.marketing)).getOrElse(record.marketing),
      modifiers = modifiers.map(_.updateRecord(record.modifiers)).getOrElse(record.modifiers),
      orders = orders.map(_.updateRecord(record.orders)).getOrElse(record.orders),
      products = products.map(_.updateRecord(record.products)).getOrElse(record.products),
      purchaseOrders = purchaseOrders.map(_.updateRecord(record.purchaseOrders)).getOrElse(record.purchaseOrders),
      reports = reports.map(_.updateRecord(record.reports)).getOrElse(record.reports),
      stockControl = stockControl.map(_.updateRecord(record.stockControl)).getOrElse(record.stockControl),
      suppliers = suppliers.map(_.updateRecord(record.suppliers)).getOrElse(record.suppliers),
      refunds = refunds.map(_.updateRecord(record.refunds)).getOrElse(record.refunds),
      giftCards = giftCards.map(_.updateRecord(record.giftCards)).getOrElse(record.giftCards),
      tips = tips.map(_.updateRecord(record.tips)).getOrElse(record.tips),
    )
}

object PermissionsUpdate {
  object Dash {
    val Manager: PermissionsUpdate =
      applyToAll(PermissionUpdate.All)

    val Employee: PermissionsUpdate =
      applyToAll(PermissionUpdate.ReadAndWrite)
        .copy(locationSettings = Some(PermissionUpdate.Empty))
  }

  object Paytouch {
    val Admin: PermissionsUpdate =
      applyToAll(PermissionUpdate.All)

    val Manager: PermissionsUpdate =
      applyToAll(PermissionUpdate.All)
        .copy(locationSettings = Some(PermissionUpdate.ReadAndEdit))

    val Employee: PermissionsUpdate =
      applyToAll(PermissionUpdate.ReadAndWrite)
        .copy(locationSettings = Some(PermissionUpdate.Empty))

    val Cashier: PermissionsUpdate =
      applyToAll(PermissionUpdate.ReadAndWrite)
        .copy(
          locationSettings = Some(PermissionUpdate.Empty),
          marketing = Some(PermissionUpdate.Empty),
          inventory = Some(PermissionUpdate.Empty),
          reports = Some(PermissionUpdate.Empty),
          suppliers = Some(PermissionUpdate.Empty),
        )

    val Temp: PermissionsUpdate =
      applyToAll(PermissionUpdate.Empty)
        .copy(
          categories = Some(PermissionUpdate.ReadOnly),
          orders = Some(PermissionUpdate.ReadAndWrite),
          customers = Some(PermissionUpdate.ReadAndWrite),
        )
  }

  val Empty: PermissionsUpdate =
    applyToAll(PermissionUpdate.Empty)

  private def applyToAll(permission: PermissionUpdate): PermissionsUpdate =
    PermissionsUpdate(
      categories = Some(permission),
      cashDrawers = Some(permission),
      customers = Some(permission),
      discounts = Some(permission),
      employeeGroups = Some(permission),
      employeePayroll = Some(permission),
      employees = Some(permission),
      employeeTimeClock = Some(permission),
      inventory = Some(permission),
      invoices = Some(permission),
      locationSettings = Some(permission),
      marketing = Some(permission),
      modifiers = Some(permission),
      orders = Some(permission),
      products = Some(permission),
      purchaseOrders = Some(permission),
      reports = Some(permission),
      stockControl = Some(permission),
      suppliers = Some(permission),
      refunds = Some(permission),
      giftCards = Some(permission),
      tips = Some(permission),
    )
}
