package io.paytouch.ordering.utils

import java.util.UUID

trait UUIDConversion {
  implicit def toUUID(s: String): UUID = UUID.fromString(s)
}
