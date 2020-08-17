package io.paytouch.ordering.data.model

import java.time.ZonedDateTime
import java.util.UUID

trait SlickRecord {

  def id: UUID

  def createdAt: ZonedDateTime

  def updatedAt: ZonedDateTime
}

trait SlickLocationRecord extends SlickRecord {
  def locationId: UUID
}

trait SlickSoftDeleteRecord extends SlickLocationRecord {
  def deletedAt: Option[ZonedDateTime]
}

trait SlickStoreRecord extends SlickRecord {
  def storeId: UUID
}

trait SlickToggleableRecord extends SlickLocationRecord {
  def active: Boolean
}
