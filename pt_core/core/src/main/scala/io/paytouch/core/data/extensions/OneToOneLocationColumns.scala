package io.paytouch.core.data.extensions

import java.util.UUID

import slick.lifted.Rep

trait OneToOneLocationColumns extends LocationIdColumn {
  def merchantId: Rep[UUID]
}
