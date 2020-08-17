package io.paytouch.core

import java.util.UUID

package object types {

  sealed trait Id {
    def id: UUID
  }
  final case class DefaultId(id: UUID = UUID.randomUUID) extends Id
}
