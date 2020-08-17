package io.paytouch.ordering.services

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities._

class TableService {
  def findByMerchantIdAndId(merchantId: UUID, tableId: UUID) =
    // For now we don't look up any data. We may need additional table data
    // later (e.g. table number), in which case we will need to call PtTables.
    Table(id = tableId)
}
