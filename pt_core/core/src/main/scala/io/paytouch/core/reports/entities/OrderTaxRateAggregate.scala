package io.paytouch.core.reports.entities

import io.paytouch.core.entities.MonetaryAmount

final case class OrderTaxRateAggregate(count: Int, amount: Option[MonetaryAmount])
