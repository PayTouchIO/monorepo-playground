package io.paytouch.ordering.clients.paytouch.core.entities

import io.paytouch.ordering.entities.MonetaryAmount

final case class MonetaryRange(min: MonetaryAmount, max: MonetaryAmount)
