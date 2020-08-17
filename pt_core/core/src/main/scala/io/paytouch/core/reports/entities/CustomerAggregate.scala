package io.paytouch.core.reports.entities

import io.paytouch.core.entities.MonetaryAmount

final case class CustomerAggregate(count: Int, spend: Option[MonetaryAmount])
