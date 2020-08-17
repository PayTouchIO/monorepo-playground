package io.paytouch.core.entities

import java.util.UUID

final case class PasswordReset(
    userId: UUID,
    token: String,
    password: String,
  )
