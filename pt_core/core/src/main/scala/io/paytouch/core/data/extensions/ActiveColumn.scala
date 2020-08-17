package io.paytouch.core.data.extensions

import slick.lifted.Rep

trait ActiveColumn {
  def active: Rep[Boolean]
}
