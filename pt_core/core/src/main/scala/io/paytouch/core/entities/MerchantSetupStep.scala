package io.paytouch.core.entities

import java.time.ZonedDateTime

final case class MerchantSetupStep(completedAt: Option[ZonedDateTime] = None, skippedAt: Option[ZonedDateTime] = None)
