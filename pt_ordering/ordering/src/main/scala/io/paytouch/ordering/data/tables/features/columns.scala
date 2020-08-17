package io.paytouch.ordering.data.tables.features

import java.time.ZonedDateTime
import java.util.UUID

import slick.lifted.Rep

trait ActiveColumn {
  def active: Rep[Boolean]
}

trait CartIdColumn {
  def cartId: Rep[UUID]
}

trait CartItemIdColumn {
  def cartItemId: Rep[UUID]
  def cartItemRelId: Rep[UUID]
}

trait DeletedAtColumn {
  def deletedAt: Rep[Option[ZonedDateTime]]
}

trait LocationIdColumn {
  def locationId: Rep[UUID]
}

trait StoreIdColumn {
  def storeId: Rep[UUID]
}
