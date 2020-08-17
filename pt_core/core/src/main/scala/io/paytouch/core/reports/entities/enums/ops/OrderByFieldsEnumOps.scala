package io.paytouch.core.reports.entities.enums.ops

import io.paytouch.core.reports.entities.enums.OrderByFieldsEnum

trait DescOrdering { self: OrderByFieldsEnum =>
  override val ordering = Ordering.Desc
}
