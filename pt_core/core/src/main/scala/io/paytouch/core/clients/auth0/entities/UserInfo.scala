package io.paytouch.core.clients.auth0.entities

import java.time.ZoneId

final case class UserInfo(
    name: String,
    email: String,
    emailVerified: Boolean = false,
    zoneinfo: Option[ZoneId] = None,
  ) {}
