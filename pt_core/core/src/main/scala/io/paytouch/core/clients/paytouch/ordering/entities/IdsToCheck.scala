package io.paytouch.core.clients.paytouch.ordering.entities

import java.util.UUID

final case class IdsToCheck(catalogIds: Seq[UUID])

final case class IdsUsage(
    accessible: IdsToCheck,
    notUsed: IdsToCheck,
    nonAccessible: IdsToCheck,
  )
