package io.paytouch.core.clients.urbanairship.entities

import java.util.UUID

final case class BeaconValue(
    uuid: UUID,
    relevantText: String,
    major: String,
    minor: String,
  )
