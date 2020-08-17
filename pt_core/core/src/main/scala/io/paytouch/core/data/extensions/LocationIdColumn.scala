package io.paytouch.core.data.extensions

import java.util.UUID

import slick.lifted.Rep

trait LocationIdColumn {
  def locationId: Rep[UUID]
}
