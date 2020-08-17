package io.paytouch.core.reports.entities

import io.paytouch.core.entities.MonetaryAmount

final case class OrderAggregate(
    count: Int,
    profit: Option[MonetaryAmount] = None,
    revenue: Option[MonetaryAmount] = None,
    waitingTimeInSeconds: Option[Int] = None,
  )
