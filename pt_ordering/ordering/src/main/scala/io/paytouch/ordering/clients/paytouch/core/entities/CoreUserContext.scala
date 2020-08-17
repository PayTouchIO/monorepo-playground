package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.{ Currency, UUID }

final case class CoreUserContext(
    id: UUID,
    merchantId: UUID,
    locationIds: Seq[UUID],
    currency: Currency,
  )
