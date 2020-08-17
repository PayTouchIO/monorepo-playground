package io.paytouch.core.filters

import java.time.LocalDateTime
import java.util.UUID

final case class ProductQuantityHistoryFilters(
    productId: UUID,
    from: Option[LocalDateTime] = None,
    to: Option[LocalDateTime] = None,
  ) extends BaseFilters
