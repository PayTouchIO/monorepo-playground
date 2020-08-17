package io.paytouch.ordering.clients.paytouch.core.entities

import io.paytouch.ordering.entities.MonetaryAmount
import io.paytouch.ordering.entities.enums.UnitType

final case class ProductLocation(
    price: MonetaryAmount,
    cost: Option[MonetaryAmount],
    unit: UnitType,
    active: Boolean,
    taxRates: Seq[TaxRate],
    stock: Option[Stock],
  )
