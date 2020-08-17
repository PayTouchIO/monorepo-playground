package io.paytouch.core.data.extensions

import java.time.ZonedDateTime

import slick.lifted.Rep

trait UaPassColumns {
  def iosPassPublicUrl: Rep[Option[String]]
  def androidPassPublicUrl: Rep[Option[String]]

  def passOptInColumn: Rep[Option[ZonedDateTime]]
}
