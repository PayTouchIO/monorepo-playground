package io.paytouch.core.data.extensions

import io.paytouch.core.data.model.enums.ReceivingObjectStatus
import slick.lifted.Rep

trait ReceivingObjectStatusColumn {
  def status: Rep[ReceivingObjectStatus]
}
