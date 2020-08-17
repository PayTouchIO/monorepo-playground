package io.paytouch.core.data.extensions

import java.time.ZonedDateTime
import java.util.UUID

import slick.lifted.Rep

trait ProductHistoryColumns {
  def productId: Rep[UUID]
  def date: Rep[ZonedDateTime]
}
