package io.paytouch.core.filters

import java.time.LocalDateTime
import java.util.UUID

import io.paytouch.core.data.model.enums.ReceivingObjectStatus
import io.paytouch.core.entities.enums.ReceivingOrderView

final case class PurchaseOrderFilters(
    locationId: Option[UUID] = None,
    supplierId: Option[UUID] = None,
    status: Option[ReceivingObjectStatus] = None,
    from: Option[LocalDateTime] = None,
    to: Option[LocalDateTime] = None,
    query: Option[String] = None,
    view: Option[ReceivingOrderView] = None,
  ) extends BaseFilters
