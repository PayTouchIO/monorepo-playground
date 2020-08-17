package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

final case class BundleSet(
    id: UUID,
    name: Option[String],
    position: Int,
    minQuantity: Int,
    maxQuantity: Int,
    options: Seq[BundleOption],
  )
