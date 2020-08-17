package io.paytouch.ordering.clients.paytouch.core.entities

import java.util.UUID

import io.paytouch.ordering.clients.paytouch.core.entities.enums.SetupType

final case class CoreMerchant(id: UUID, setupType: SetupType)
