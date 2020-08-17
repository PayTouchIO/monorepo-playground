package io.paytouch.core.reports.entities

import java.util.UUID

final case class EmployeeSales(
    id: UUID,
    firstName: String,
    lastName: String,
    data: SalesAggregate,
  )
