package io.paytouch.core.filters

import java.util.UUID

final case class SessionFilters(userId: UUID, oauthAppName: Option[String] = None) extends BaseFilters
